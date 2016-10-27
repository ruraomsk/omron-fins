/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class FwRegister
{

    private int controller = 0;
    private int uId = 0;
    private boolean info = true;
    private int type = 0; // 0-bool  1-int  2-float
    private int len = 1;

    public FwRegister(int controller, int uId, int type)
    {
        this.controller = controller;
        this.uId = uId;
        this.type = type;
        if (type == FwUtil.FP_TYPE_FLOAT) {
            this.len = 4;
        }
        if (type == FwUtil.FP_TYPE_INTGER) {
            this.len = 2;
        }
    }

    public FwRegister(int controller, int uId, int type, int len)
    {
        this.controller = controller;
        this.uId = uId;
        this.type = type;
        this.len = len;
    }

    public FwRegister(int controller, boolean diag, int uId, int type)
    {
        this.controller = controller;
        this.uId = uId;
        this.type = type;
        info = diag;
    }

    public FwRegister(int controller, boolean diag, int uId, int type, int len)
    {
        this.controller = controller;
        this.uId = uId;
        this.type = type;
        info = diag;
        this.len = len;
    }

    public boolean isDiag()
    {
        return !info;
    }

    public boolean isInfo()
    {
        return info;
    }

    /**
     * @return the controller
     */
    public int getController()
    {
        return controller;
    }

    /**
     * @param controller the controller to set
     */
    public void setController(int controller)
    {
        this.controller = controller;
    }

    /**
     * @return the uId
     */
    public int getuId()
    {
        return uId;
    }

    /**
     * @param uId the uId to set
     */
    public void setuId(int uId)
    {
        this.uId = uId;
    }

    /**
     * @return the type
     */
    public int getType()
    {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(int type)
    {
        this.type = type;
    }

    public long getKey()
    {
        return ((info ? FwUtil.FP_FLAGINFO : 0L) + ((long) controller) << 16) + uId;
    }

    public static long makeKey(int controller, int uId, boolean info)
    {
        return ((info ? FwUtil.FP_FLAGINFO : 0L) + ((long) controller) << 16) + uId;
    }

    @Override
    public String toString()
    {
        return (Integer.toString(getController()) + " " + (isInfo() ? "И" : "Д") + " uId=" + Integer.toString(getuId()) + " " + Integer.toString(getType()));
    }

    /**
     * @return the len
     */
    public int getLen()
    {
        return len;
    }

    /**
     * @param len the len to set
     */
    public void setLen(int len)
    {
        this.len = len;
    }

}
