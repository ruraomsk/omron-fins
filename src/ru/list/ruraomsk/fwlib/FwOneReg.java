/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

import java.util.Date;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class FwOneReg
{

    private long date;
    private FwRegister reg = null;
    private Object value = null;
    private byte good = FwUtil.FP_DATA_NOGOOD;

    public FwOneReg(long date, FwRegister reg, Object value, byte good)
    {
        this.date = date;
        this.reg = reg;
        this.value = value;
        this.good = good;
    }

    public FwOneReg(long date, FwRegister reg, Object value)
    {
        this.date = date;
        this.reg = reg;
        this.value = value;
        this.good=FwUtil.FP_DATA_GOOD;
    }

    public FwOneReg(long date, FwRegister reg)
    {
        this.date = date;
        this.reg = reg;
        switch (reg.getType()) {
            case FwUtil.FP_TYPE_BOOL:
                this.value = false;
                break;
            case FwUtil.FP_TYPE_INTGER:
                this.value = 0;
                break;
            case FwUtil.FP_TYPE_FLOAT:
                this.value = 0.0f;
                break;
        }
    }

    @Override
    public String toString()
    {
        return (new Date(date).toString() + " " + (reg.isInfo() ? "И" : "Д") + " uId=" + Integer.toString(reg.getuId())
                + " value=" + value.toString() + " good=0x" + Integer.toHexString(good));
    }

    /**
     * @return the date
     */
    public long getDate()
    {
        return date;
    }

    /**
     * @param date the date to set
     */
    public void setDate(long date)
    {
        this.date = date;
    }

    /**
     * @return the reg
     */
    public FwRegister getReg()
    {
        return reg;
    }

    /**
     * @param reg the reg to set
     */
    public void setReg(FwRegister reg)
    {
        this.reg = reg;
    }

    /**
     * @return the value
     */
    public Object getValue()
    {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(Object value)
    {
        this.value = value;
    }

    /**
     * @return the good
     */
    public byte getGood()
    {
        return good;
    }

    /**
     * @param good the good to set
     */
    public void setGood(byte good)
    {
        this.good = good;
    }

    public int getuId()
    {
        return reg.getuId();
    }

    void setBuffer(byte[] buffer, int pos)
    {
        switch (getReg().getType()) {
            case FwUtil.FP_TYPE_BOOL:
                if (getReg().getLen() == 1) {
                    buffer[pos] = (byte) (((boolean) getValue()) ? 0x1 : 0x0);
                    break;
                }
                if (getReg().getLen() == 2) {
                    FwUtil.IntToBuff(buffer, pos, ((boolean) getValue()) ? 0x1 : 0x0);
                    break;
                }
                if (getReg().getLen() == 4) {
                    FwUtil.floatToBuff(buffer, pos, (boolean) getValue() ? 1.0f : 0.0f);
                    break;
                }

            case FwUtil.FP_TYPE_INTGER:
                if (getReg().getLen() == 2) {
                    FwUtil.IntToBuff(buffer, pos, (int) getValue());
                    break;
                }
                if (getReg().getLen() == 4) {
                    FwUtil.floatToBuff(buffer, pos, (int) getValue());
                    break;
                }
            case FwUtil.FP_TYPE_FLOAT:
                FwUtil.floatToBuff(buffer, pos, (float) getValue());
                break;
        }
    }

    void getBuffer(byte[] buffer, int pos)
    {
        switch (getReg().getType()) {
            case FwUtil.FP_TYPE_BOOL:
                if (getReg().getLen() == 1) {
                    setValue((buffer[pos] != 0));
                    break;
                }
                if (getReg().getLen() == 2) {
                    setValue((FwUtil.ToShort(buffer, pos) != 0));
                    break;
                }
                if (getReg().getLen() == 4) {
                    setValue(FwUtil.ToFloat(buffer, pos) != 0.0);
                    break;
                }

            case FwUtil.FP_TYPE_INTGER:
                if (getReg().getLen() == 2) {
                    setValue(FwUtil.ToShort(buffer, pos));
                    break;
                }
                if (getReg().getLen() == 4) {
                    setValue((int) FwUtil.ToFloat(buffer, pos));
                    break;
                }

            case FwUtil.FP_TYPE_FLOAT:
                setValue(FwUtil.ToFloat(buffer, pos));
                break;
        }
    }
}
