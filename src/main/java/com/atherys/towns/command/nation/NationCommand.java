package com.atherys.towns.command.nation;

import com.atherys.core.command.PlayerCommand;
import com.atherys.core.command.annotation.*;
import com.atherys.towns.AtherysTowns;
import com.atherys.towns.command.nation.admin.DisbandNationCommand;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;

import javax.annotation.Nonnull;

@Aliases({"nation", "n"})
@Description("Base nation command.")
@Permission("atherystowns.nation.base")
@Children({
        CreateNationCommand.class,
        DisbandNationCommand.class,
        AddNationAllyCommand.class,
        AddNationEnemyCommand.class,
        AddNationNeutralCommand.class,
        NationAddActorPermissionCommand.class,
        NationInfoCommand.class,
        NationRemoveActorPermissionCommand.class,
        NationListCommand.class,
        DepositNationCommand.class,
        WithdrawNationCommand.class,
        SetNationCapitalCommand.class,
        SetNationNameCommand.class,
        SetNationDescriptionCommand.class,
        NationRoleCommand.class,
        SetNationTaxCommand.class
})
@HelpCommand(title = "Nation Help", command = "help")
public class NationCommand implements PlayerCommand {
    @Nonnull
    @Override
    public CommandResult execute(@Nonnull Player source, @Nonnull CommandContext args) throws CommandException {
        AtherysTowns.getInstance().getNationFacade().sendPlayerNationInfo(source);
        return CommandResult.success();
    }
}
