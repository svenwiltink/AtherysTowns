package com.atherys.towns.permission;

import com.atherys.towns.service.PermissionService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.Subject;

import java.util.Set;

@Singleton
public class TownsContextCalculator implements ContextCalculator<Subject> {

    @Inject
    private PermissionService permissionService;

    public TownsContextCalculator() {
    }

    @Override
    public void accumulateContexts(Subject calculable, Set<Context> accumulator) {
        permissionService.accumulateContexts(calculable, accumulator);
    }

    @Override
    public boolean matches(Context context, Subject calculable) {
        return PermissionService.NATION_CONTEXT_KEY.equals(context.getKey()) || PermissionService.TOWN_CONTEXT_KEY.equals(context.getKey());
    }
}
