
package com.example.gamigosjava.data.model;

import com.tickaroo.tikxml.annotation.Element;
import com.tickaroo.tikxml.annotation.Xml;
import java.util.List;

@Xml(name = "items")
public class SearchResponse {
    @Element(name = "item")
    public List<SearchItem> items;
}
