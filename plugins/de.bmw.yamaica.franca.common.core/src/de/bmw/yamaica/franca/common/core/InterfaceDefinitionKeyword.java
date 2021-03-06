/* Copyright (C) 2013-2015 BMW Group
 * Author: Manfred Bathelt (manfred.bathelt@bmw.de)
 * Author: Juergen Gehring (juergen.gehring@bmw.de)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package de.bmw.yamaica.franca.common.core;


public enum InterfaceDefinitionKeyword
{
    ARRAY("array"),
    ATTRIBUTE("attribute"),
    BOOLEAN("Boolean"),
    BROADCAST("broadcast"),
    BYTE_BUFFER("ByteBuffer"),
    CALL("call"),
    CONTRACT("contract"),
    DOUBLE("Double"),
    ENUMERATION("enumeration"),
    ERROR("error"),
    EXTENDS("extends"),
    FIRE_AND_FORGET("fireAndForget"),
    FLOAT("Float"),
    FROM("from"),
    IMPORT("import"),
    IN("in"),
    INITIAL("initial"),
    INTERFACE("interface"),
    INT16("Int16"),
    INT32("Int32"),
    INT64("Int64"),
    INT8("Int8"),
    IS("is"),
    MAP("map"),
    MAJOR("major"),
    METHOD("method"),
    MINOR("minor"),
    MODEL("model"),
    NO_SUBSCRIPTIONS("noSubscriptions"),
    OF("of"),
    ON("on"),
    OUT("out"),
    PACKAGE("package"),
    PSM("PSM"),
    RESPOND("respond"),
    READ_ONLY("readonly"),
    SELECTIVE("selective"),
    SET("set"),
    SIGNAL("signal"),
    STATE("state"),
    STRING("String"),
    STRUCT("struct"),
    TYPE_COLLECTION("typeCollection"),
    TYPEDEF("typedef"),
    UINT16("UInt16"),
    UINT32("UInt32"),
    UINT64("UInt64"),
    UINT8("UInt8"),
    UNDEFINED("undefined"),
    UNION("union"),
    UPDATE("update"),
    VARS("vars"),
    VERSION("version");

    public static InterfaceDefinitionKeyword getByName(String name)
    {
        for (InterfaceDefinitionKeyword francaKeyword : values())
        {
            if (francaKeyword.getName().equals(name))
            {
                return francaKeyword;
            }
        }

        return null;
    }

    public static boolean isKeyword(String name)
    {
        return null != getByName(name);
    }

    private final String name;

    private InterfaceDefinitionKeyword(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
