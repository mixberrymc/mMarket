package me.mixberry.mMarket.commands;

import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.command.ReloadCommand;
import org.mineacademy.fo.command.SimpleCommandGroup;

@AutoRegister
public final class mMarketCommand extends SimpleCommandGroup {

    public mMarketCommand() {
        super("mmarket/mm");
    }

    @Override
    protected String getCredits() {
        return "&fMade with &c❤ &fby &bmixberry";
    }

    @Override
    protected void registerSubcommands() {
        registerSubcommand(new ReloadCommand("mmarket.admin"));
    }


}
