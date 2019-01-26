package com.atherys.towns.service;

import com.atherys.towns.api.permission.Subject;
import com.atherys.towns.api.permission.Actor;
import com.atherys.towns.api.permission.Permission;
import com.atherys.towns.api.permission.nation.NationPermissions;
import com.atherys.towns.api.permission.town.TownPermissions;
import com.atherys.towns.api.permission.world.WorldPermissions;
import com.atherys.towns.entity.PermissionNode;
import com.atherys.towns.persistence.PermissionRepository;
import com.google.inject.Singleton;
import org.spongepowered.api.registry.CatalogRegistryModule;

import javax.inject.Inject;
import java.util.*;

@Singleton
public class PermissionService implements CatalogRegistryModule<Permission> {

    private PermissionRepository permissionRepository;

    private Map<String, Permission> permissions = new HashMap<>();

    @Inject
    PermissionService(
            PermissionRepository permissionRepository
    ) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public void registerDefaults() {
        put(
                NationPermissions.INVITE_TOWN,
                NationPermissions.KICK_TOWN,
                NationPermissions.ADD_PERMISSION,
                NationPermissions.REVOKE_PERMISSION,
                NationPermissions.WITHDRAW_FROM_BANK,
                NationPermissions.DEPOSIT_INTO_BANK,
                NationPermissions.SET_NAME,
                NationPermissions.SET_DESCRIPTION,
                NationPermissions.SET_FREELY_JOINABLE,
                NationPermissions.ADD_ALLY,
                NationPermissions.REMOVE_ALLY,
                NationPermissions.DECLARE_WAR,
                NationPermissions.DECLARE_PEACE,
                NationPermissions.TRANSFER_LEADERSHIP,
                NationPermissions.CHAT
        );

        put(
                TownPermissions.INVITE_RESIDENT,
                TownPermissions.KICK_RESIDENT,
                TownPermissions.CLAIM_PLOT,
                TownPermissions.UNCLAIM_PLOT,
                TownPermissions.ADD_PERMISSION,
                TownPermissions.REVOKE_PERMISSION,
                TownPermissions.WITHDRAW_FROM_BANK,
                TownPermissions.DEPOSIT_INTO_BANK,
                TownPermissions.LEAVE_NATION,
                TownPermissions.JOIN_NATION,
                TownPermissions.SET_NAME,
                TownPermissions.SET_DESCRIPTION,
                TownPermissions.SET_MOTD,
                TownPermissions.SET_COLOR,
                TownPermissions.SET_FREELY_JOINABLE,
                TownPermissions.SET_SPAWN,
                TownPermissions.SET_PVP,
                TownPermissions.TRANSFER_LEADERSHIP,
                TownPermissions.CHAT
        );

        put(
                WorldPermissions.BUILD,
                WorldPermissions.DESTROY,
                WorldPermissions.DAMAGE_NONPLAYERS,
                WorldPermissions.DAMAGE_PLAYERS,
                WorldPermissions.INTERACT_CHESTS,
                WorldPermissions.INTERACT_DOORS,
                WorldPermissions.INTERACT_REDSTONE,
                WorldPermissions.INTERACT_ENTITIES
        );
    }

    private void put(Permission permission) {
        permissions.put(permission.getId(), permission);
    }

    private void put(Permission... permissions) {
        for (Permission permission : permissions) {
            put(permission);
        }
    }

    public void permit(Actor actor, Subject subject, Set<Permission> permissions) {
        permissions.forEach(permission -> permit(actor, subject, permission));
    }

    public void revoke(Actor actor, Subject subject, Set<Permission> permissions) {
        permissions.forEach(permission -> remove(actor, subject, permission, true));
    }

    public void permit(Actor user, Subject subject, Permission permission) {
        permit(user, subject, permission, true);
    }

    public void remove(Actor actor, Subject subject, Permission permission, boolean permitted) {
        permissionRepository.deleteOne(createPermissionNode(actor, subject, permission, permitted));
    }

    /**
     * Create a new PermissionNode object and store it in the database
     *
     * @param user
     * @param subject
     * @param permission
     * @param permitted
     */
    public void permit(Actor user, Subject subject, Permission permission, boolean permitted) {
        permissionRepository.saveOne(createPermissionNode(user, subject, permission, permitted));
    }

    public PermissionNode createPermissionNode(Actor actor, Subject subject, Permission permission, boolean permitted) {
        PermissionNode node = new PermissionNode();
        node.setUserId(formatUserId(actor));
        node.setContextId(formatContextId(subject));
        node.setPermission(permission);
        node.setPermitted(permitted);

        return node;
    }

    /**
     * Check the database for PermissionNode objects which may grant the actor the provided permission, within the
     * context of the subject.
     * <br><br>
     * Firstly, a check is made for what are known as "explicit" permissions. An explicit permission is one where
     * there is a PermissionNode which grants this specific Actor to act upon this specific Subject in the specified
     * way.
     * <br><br>
     * If an explicit PermissionNode is found which does the opposite, to explicitly deny the Actor this permission,
     * then no further checks will be made.
     * <br><br>
     * If no such PermissionNode is found, the next check to follow is to see whether the Actor is transiently
     * permitted to execute this action.
     * <br><br>
     * A transient permission is when the Actor is permitted the action upon the provided Subject's
     * parent object instead.
     * <br><br>
     * For example, if a Resident is permitted to destroy within a Town, then that means that they are allowed
     * to destroy within all Plots belonging to said town. This is transient, because the permission's subject is
     * not the Plot itself, but rather the Plot's parent object - the Town.
     * <br><br>
     * This method is actually recursive, in that it will call itself to check for an explicit permission where the
     * Actor is the same, but the Subject is now the previous Subject's parent.
     * <br><br>
     * If no transient PermissionNode is found either, then the next check will see if the Actor is itself
     * also a Subject. If so, a check will be made if the Actor's parent Subject ( if any ) is permitted to
     * execute the action in question upon the provided Subject. All previous checks mentioned will also be done.
     * <br><br>
     *
     * @param actor      The actor ( A resident/town/nation )
     * @param subject    The subject ( A plot/town/nation )
     * @param permission the permission ( also known as an "action" ).
     * @return Whether the Actor is permitted to execute the specified action upon the Subject.
     */
    public boolean isPermitted(Actor actor, Subject subject, Permission permission) {

        String userId = formatUserId(actor);
        String contextId = formatContextId(subject);

        // check for an explicit permission
        Optional<PermissionNode> any = permissionRepository.findAnyBy(userId, contextId, permission);

        // if explicitly permitted, return
        if (any.isPresent()) return any.get().isPermitted();

        // check for transient permissions
        boolean transientPermitted = subject.hasParent() && isPermitted(actor, subject.getParent(), permission);

        // if transiently permitted, return
        if (transientPermitted) return transientPermitted;

        // if the user being checked is also a subject, check it's parents for explicit and transient permissions
        if (actor instanceof Subject) {

            if (!((Subject) actor).hasParent()) return false;

            Subject parent = ((Subject) actor).getParent();

            return (parent instanceof Actor) && isPermitted((Actor) parent, subject, permission);

        }

        return false;

    }

    public void ifPermitted(Actor actor, Subject subject, Permission permission, Runnable action) {
        if (isPermitted(actor, subject, permission)) action.run();
    }

    private String formatUserId(Actor user) {
        return String.format("%s{%s}", user.getClass().getSimpleName(), user.getUniqueId().toString());
    }

    private String formatContextId(Subject subject) {
        return String.format("%s{%s}", subject.getClass().getSimpleName(), subject.getUniqueId().toString());
    }

    @Override
    public Optional<Permission> getById(String id) {
        return Optional.empty();
    }

    @Override
    public Collection<Permission> getAll() {
        return null;
    }
}