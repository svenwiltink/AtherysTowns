package com.atherys.towns.facade;

import com.atherys.towns.api.command.TownsCommandException;
import com.atherys.towns.model.PlotSelection;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Singleton
public class PlotSelectionFacade {

    private final Map<UUID, PlotSelection> selections = new HashMap<>();

    @Inject
    TownsMessagingFacade townMsg;

    private PlotSelection getOrCreateSelection(Player player) {
        if (selections.containsKey(player.getUniqueId())) {
            return selections.get(player.getUniqueId());
        } else {
            PlotSelection plotSelection = new PlotSelection();
            selections.put(player.getUniqueId(), plotSelection);
            return plotSelection;
        }
    }

    private void selectPointAAtLocation(Player player, Location<World> location) {
        getOrCreateSelection(player).setPointA(location);
    }

    private void selectPointBAtLocation(Player player, Location<World> location) {
        getOrCreateSelection(player).setPointB(location);
    }

    public void clearSelection(Player player) {
        selections.remove(player.getUniqueId());
        townMsg.info(player, "You have cleared your selection.");
    }

    public void selectPointAFromPlayerLocation(Player player) {
        selectPointAAtLocation(player, player.getLocation());
        sendPointSelectionMessage(player, "A");
    }

    public void selectPointBFromPlayerLocation(Player player) {
        selectPointBAtLocation(player, player.getLocation());
        sendPointSelectionMessage(player, "B");
    }

    private void sendPointSelectionMessage(Player player, String point) {
        Location<World> location = player.getLocation();
        townMsg.info(player, "You have selected point", TextColors.GOLD, " ", point, " ", TextColors.DARK_GREEN,
                "at ", location.getBlockX(), ", ", location.getBlockY(), ", ", location.getBlockZ(), ".");
    }

    /**
     * Validate a plot selection.
     * @param selection
     * @param player
     * @throws CommandException
     */
    public void validatePlotSelection(PlotSelection selection, Player player) throws CommandException {
        if (selection == null) {
            throw new TownsCommandException("Plot selection is null.");
        }

        if (!selection.isComplete()) {
            throw new TownsCommandException("Plot selection is incomplete.");
        }
    }

    public PlotSelection getCurrentPlotSelection(Player player) {
        return getOrCreateSelection(player);
    }

    public PlotSelection getValidPlayerPlotSelection(Player source) throws CommandException {
        PlotSelection selection = getOrCreateSelection(source);
        validatePlotSelection(selection, source);
        return selection;
    }
}
