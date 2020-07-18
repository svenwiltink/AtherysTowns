package com.atherys.towns.facade;

import com.atherys.core.economy.Economy;
import com.atherys.towns.AtherysTowns;
import com.atherys.towns.TownsConfig;
import com.atherys.towns.api.command.TownsCommandException;
import com.atherys.towns.config.RaidConfig;
import com.atherys.towns.model.RaidPoint;
import com.atherys.towns.model.entity.Town;
import com.atherys.towns.service.ResidentService;
import com.atherys.towns.service.TownRaidService;
import com.flowpowered.math.vector.Vector3d;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.AreaEffectCloud;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.spongepowered.api.text.format.TextColors.*;

@Singleton
public class TownRaidFacade {

    @Inject
    TownsConfig config;

    @Inject
    TownFacade townFacade;

    @Inject
    TownsMessagingFacade townsMsg;

    @Inject
    TownRaidService townRaidService;

    @Inject
    ResidentService residentService;

    public TownRaidFacade() {

    }

    public boolean isEntityRaidPoint(Entity entity) {
        return townRaidService.isIdRaidEntity(entity.getUniqueId());
    }

    public void validateRaid(Town town, Transform<World> transform) throws TownsCommandException {
        Location<World> location = transform.getLocation();
        RaidConfig raidConfig = config.RAID;

        if (AtherysTowns.economyIsEnabled()) {
            Optional<Account> townBank = Economy.getAccount(town.getBank().toString());
            if (townBank.isPresent() && townBank.get().getBalance(config.DEFAULT_CURRENCY).doubleValue() < raidConfig.RAID_COST) {
                throw new TownsCommandException("Your town bank does not have enough money to create a raid point!");
            }
        }

        if (townRaidService.isTownRaidActive(town)) {
            throw new TownsCommandException("Your town already has an active raid point!");
        }

        if (townRaidService.isLocationTaken(location)) {
            throw new TownsCommandException("This location is already occupied by another town's raid point!");
        }

        if (!townRaidService.hasCooldownPeriodPassed(town)) {
            throw new TownsCommandException("Raid Cooldown is still in effect!");
        }
    }

    public Set<UUID> spawnPortalParticles(Transform<World> transform, Player player) {
        AreaEffectCloud cloud = (AreaEffectCloud) player.getWorld().createEntity(EntityTypes.AREA_EFFECT_CLOUD, transform.getPosition().add(0, -0.1, 0));
        cloud.offer(Keys.AREA_EFFECT_CLOUD_RADIUS, 2.0);
        cloud.offer(Keys.AREA_EFFECT_CLOUD_PARTICLE_TYPE, ParticleTypes.CLOUD);
        cloud.offer(Keys.AREA_EFFECT_CLOUD_COLOR, Color.RED);
        cloud.offer(Keys.AREA_EFFECT_CLOUD_DURATION, (int) config.RAID.RAID_DURATION.getSeconds() * 20);

        AreaEffectCloud cloud2 = (AreaEffectCloud) player.getWorld().createEntity(EntityTypes.AREA_EFFECT_CLOUD, transform.getPosition().add(0, 0.5, 0));
        cloud2.offer(Keys.AREA_EFFECT_CLOUD_RADIUS, 2.0);
        cloud2.offer(Keys.AREA_EFFECT_CLOUD_PARTICLE_TYPE, ParticleTypes.ENCHANTING_GLYPHS);
        cloud2.offer(Keys.AREA_EFFECT_CLOUD_DURATION, (int) config.RAID.RAID_DURATION.getSeconds() * 20);

        player.getWorld().spawnEntity(cloud);
        player.getWorld().spawnEntity(cloud2);

        Set<UUID> particleSet = new HashSet<>();
        particleSet.add(cloud.getUniqueId());
        particleSet.add(cloud2.getUniqueId());

        return particleSet;
    }

    public UUID spawnRaidPoint(Transform<World> transform, Player player) {
        Entity entity = player.getWorld().createEntity(EntityTypes.HUMAN, transform.getPosition().add(0, 1, -2));

        entity.offer(Keys.AI_ENABLED, false);
        entity.offer(Keys.DISPLAY_NAME, Text.of("Hired Mage"));
        entity.offer(Keys.MAX_HEALTH, config.RAID.RAID_HEALTH);
        entity.offer(Keys.HEALTH, config.RAID.RAID_HEALTH);
        entity.offer(Keys.SKIN_UNIQUE_ID, UUID.fromString("b1759db2-3b7f-4d5d-9155-70aca6e94cba"));

        player.getWorld().spawnEntity(entity);

        return entity.getUniqueId();
    }

    private Text formatRaidLocation(Transform<World> transform) {
        Location<World> location = transform.getLocation();
        return Text.of(GOLD, "x: ", location.getBlockX(), ", y: ", location.getBlockY(), ", z: ", location.getBlockZ());
    }

    private Text formatDurationLeft(LocalDateTime time, Duration duration) {
        Duration durationBetweenPresent = Duration.between(time, LocalDateTime.now());
        Duration durationLeft = duration.minus(durationBetweenPresent);
        if (durationLeft.toMinutes() >= 1) {
            return Text.of(GOLD, durationLeft.toMinutes(), " minute(s)");
        }
        return Text.of(GOLD, durationLeft.getSeconds(), " second(s)");
    }

    public double getRaidHealth(World world, RaidPoint point) {
        Optional<Entity> entity = world.getEntity(point.getRaidPointUUID());
        if (entity.isPresent()) {
            return entity.map(value -> Math.round(value.get(Keys.HEALTH).orElse(0.00))).get();
        }
        return 0;
    }

    public void sendRaidPointInfo(Player player) throws TownsCommandException {
        Town town = townFacade.getPlayerTown(player);
        boolean raidExists = townRaidService.isTownRaidActive(town);
        Optional<RaidPoint> point = townRaidService.getTownRaidPoint(town);
        Text status = raidExists ? Text.of(GREEN, "True") : Text.of(RED, "False");
        Text pointLocation = raidExists ? formatRaidLocation(point.get().getPointTransform()) : Text.of(RED, "No Raid in Progress");
        Text pointHealth = raidExists ? Text.of(GOLD, getRaidHealth(player.getWorld(), point.get())) : Text.of(RED, "No Raid in Progress");
        Text durationLeft = raidExists ? formatDurationLeft(point.get().getCreationTime(), config.RAID.RAID_DURATION) : Text.of(RED, "No Raid in Progress");
        Text cooldown = townRaidService.hasCooldownPeriodPassed(town) ? Text.of(GREEN, "No Cooldown Remaining") : formatDurationLeft(town.getLastRaidCreationDate(), config.RAID.RAID_COOLDOWN);

        Text.Builder raidText = Text.builder();

        raidText
                .append(townsMsg.createTownsHeader("Raid Point"));

        raidText
                .append(Text.of(DARK_GREEN, "Raid in Progress: ", status, Text.NEW_LINE))
                .append(Text.of(DARK_GREEN, "Raid Location: ", pointLocation, Text.NEW_LINE))
                .append(Text.of(DARK_GREEN, "Point Health: ", pointHealth, Text.NEW_LINE))
                .append(Text.of(DARK_GREEN, "Duration Left: ", durationLeft, Text.NEW_LINE))
                .append(Text.of(DARK_GREEN, "Cooldown: ", cooldown));

        player.sendMessage(raidText.build());
    }

    public Transform<World> getRaidPointTransform(Player player) {
        Vector3d playerVector = player.getTransform().getPosition();
        Location<World> highLocation = player.getLocation().asHighestLocation();
        Location<World> newLocation = new Location<>(player.getWorld(), playerVector.getX(), highLocation.getBlockY(), playerVector.getZ());
        return player.getTransform().setLocation(newLocation);
    }

    public void createRaidPoint(Player player) throws TownsCommandException {
        Town town = townFacade.getPlayerTown(player);
        Transform<World> transform = getRaidPointTransform(player);
        validateRaid(town, transform);

        UUID entityId = spawnRaidPoint(transform, player);
        Set<UUID> particleEffects = spawnPortalParticles(transform, player);
        if (AtherysTowns.economyIsEnabled()) {
            Economy.getAccount(town.getBank().toString()).ifPresent(account -> {
                Cause cause = Sponge.getCauseStackManager().getCurrentCause();
                account.withdraw(config.DEFAULT_CURRENCY, BigDecimal.valueOf(config.RAID.RAID_COST), cause);
            });
        }
        townRaidService.createRaidPointEntry(transform, town, entityId, particleEffects);

        townFacade.getOnlineTownMembers(town).forEach(member -> townsMsg.info(member, "A town raid point has been spawned!"));
    }

    public void onRaidPointDeath(DestructEntityEvent.Death event) {
        RaidPoint point = townRaidService.getRaidPoint(event.getTargetEntity().getUniqueId());
        point.getParticleUUIDs().forEach(uuid -> townRaidService.removeEntity(point.getPointTransform().getExtent(), uuid));
        townRaidService.removeRaidPoint(point.getRaidPointUUID());
    }

    public boolean onPlayerSpawn(RespawnPlayerEvent event) {
        Town town = residentService.getOrCreate(event.getOriginalPlayer()).getTown();
        if (townRaidService.isTownRaidActive(town)) {
            Optional<RaidPoint> point = townRaidService.getTownRaidPoint(town);
            point.ifPresent(raidPoint -> {
                event.setToTransform(raidPoint.getPointTransform());
                event.getOriginalPlayer().getWorld().getEntity(raidPoint.getRaidPointUUID()).ifPresent(entity -> {
                    double initialHealth = entity.get(Keys.HEALTH).orElse(0.0);
                    entity.offer(Keys.HEALTH, initialHealth - config.RAID.RAID_DAMAGE_PER_SPAWN);
                });
            });
            return true;
        }
        return false;
    }


}