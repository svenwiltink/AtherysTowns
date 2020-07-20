package com.atherys.towns.model.entity;

import com.atherys.towns.persistence.converter.TextConverter;
import org.spongepowered.api.text.Text;

import javax.persistence.*;

@Entity
public class TownPlot extends Plot {

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "town_id")
    private Town town;

    @Convert(converter = TextConverter.class)
    private Text name;

    @ManyToOne(fetch = FetchType.EAGER)
    private Resident owner;

    public Town getTown() {
        return town;
    }

    public void setTown(Town town) {
        this.town = town;
    }

    public Text getName() {
        return name;
    }

    public void setName(Text name) {
        this.name = name;
    }

    public Resident getOwner() {
        return owner;
    }

    public void setOwner(Resident owner) {
        this.owner = owner;
    }
}
