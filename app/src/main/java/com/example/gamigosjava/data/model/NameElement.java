package com.example.gamigosjava.data.model;

import androidx.annotation.Nullable;
import com.tickaroo.tikxml.annotation.Attribute;
import com.tickaroo.tikxml.annotation.Xml;

@Xml(name = "name")
public class NameElement {

    // e.g., "primary" or "alternate"
    @Nullable
    @Attribute(name = "type")
    public String type;

    // the actual title text
    @Nullable
    @Attribute(name = "value")
    public String value;

    // TikXml needs a public no-arg constructor
    public NameElement() { }
}
