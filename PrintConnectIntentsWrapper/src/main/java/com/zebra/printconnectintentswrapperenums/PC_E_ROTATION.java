package com.zebra.printconnectintentswrapperenums;

public enum PC_E_ROTATION {
    ZERO("0"),
    NINETY("90"),
    ONEHUNDREDEIGHTY("180"),
    TWOHUNDREDSEVENTY("270");

    private String enumString;
    private PC_E_ROTATION(String confName)
    {
        this.enumString = confName;
    }

    @Override
    public String toString()
    {
        return enumString;
    }

    public int toInt()
    {
        return Integer.valueOf(enumString);
    }

    static public PC_E_ROTATION getRotation(int rotation)
    {
        switch (rotation)
        {
            case 0:
                return ZERO;
            case 90:
                return NINETY;
            case 180:
                return ONEHUNDREDEIGHTY;
            case 270:
                return TWOHUNDREDSEVENTY;
            default:
                return ZERO;
        }
    }
}
