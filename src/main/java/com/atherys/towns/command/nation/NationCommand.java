package com.atherys.towns.command.nation;

import com.atherys.core.command.annotation.Aliases;
import com.atherys.core.command.annotation.Children;
import com.atherys.core.command.annotation.Description;
import com.atherys.core.command.annotation.Permission;
import com.atherys.towns.AtherysTowns;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

@Aliases("nation")
@Description("Base nation command.")
@Permission("atherystowns.nation")
@Children({
        CreateNationCommand.class,
        NationAddActorPermissionCommand.class,
        NationInfoCommand.class,
        NationRemoveActorPermissionCommand.class,
        SetNationDescriptionCommand.class,
        SetNationNameCommand.class,
        SetNationTaxCommand.class,
        SetNationCapitalCommand.class,
        AddNationAllyCommand.class
})
public class NationCommand implements CommandExecutor {
    @Override
    public CommandResult execute(CommandSource src, CommandContext arg) throws CommandException {
        if (src instanceof Player) {
            AtherysTowns.getInstance().getNationFacade().sendPlayerNationInfo((Player) src);
        }
        return CommandResult.success();
    }
}
