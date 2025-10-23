// ValueElement.java
package com.example.gamigosjava.data.model;

import com.tickaroo.tikxml.annotation.Attribute;
import com.tickaroo.tikxml.annotation.Xml;

@Xml
public class ValueElement {
    @Attribute(name = "value")
    public Integer value;
}
