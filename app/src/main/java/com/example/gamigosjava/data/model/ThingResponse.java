package com.example.gamigosjava.data.model;

import java.util.List;
import com.tickaroo.tikxml.annotation.Xml;
import com.tickaroo.tikxml.annotation.Element;
import com.tickaroo.tikxml.annotation.Attribute;

@Xml(name = "items")
public class ThingResponse {
    @Element(name = "item")
    public List<BGGItem> items;
}
