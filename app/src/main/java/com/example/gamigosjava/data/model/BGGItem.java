// BggItem.java
package com.example.gamigosjava.data.model;

import androidx.annotation.Nullable;
import com.tickaroo.tikxml.annotation.Attribute;
import com.tickaroo.tikxml.annotation.Element;
import com.tickaroo.tikxml.annotation.PropertyElement;
import com.tickaroo.tikxml.annotation.Xml;

import java.util.List;

@Xml(name = "item")
public class BGGItem {
    @Attribute(name = "id")
    public String id;

    // <image>https://...</image>
    @Nullable @PropertyElement(name = "image")
    public String image;

    // <thumbnail>https://...</thumbnail>
    @Nullable @PropertyElement(name = "thumbnail")
    public String thumbnail;

    // <name type="primary" value="Catan"/> entries

    @Nullable
    @Element(name = "name")
    public List<NameElement> names;

    // <minplayers value="2" />
    @Nullable @Element(name = "minplayers")
    public ValueElement minPlayers;

    // <maxplayers value="4" />
    @Nullable @Element(name = "maxplayers")
    public ValueElement maxPlayers;

    // <playingtime value="45" />
    @Nullable @Element(name = "playingtime")
    public ValueElement playingTime;
}
