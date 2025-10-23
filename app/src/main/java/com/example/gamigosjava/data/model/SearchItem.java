
package com.example.gamigosjava.data.model;

import com.tickaroo.tikxml.annotation.Attribute;
import com.tickaroo.tikxml.annotation.Element;
import com.tickaroo.tikxml.annotation.Xml;
import java.util.List;

@Xml(name = "item")
public class SearchItem {
    @Attribute(name = "id") public String id;

    // search can return multiple <name .../>
    @Element(name = "name")
    public List<NameElement> names;
}
