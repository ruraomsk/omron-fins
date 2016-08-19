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
public class FwSyncTime extends FwBaseMess
{
    private int controller=0;
    private Date datelast=null;
    private Date datenow=null;
    private final byte functioncode=FwUtil.FP_CODE_91H;
    
    /**
     * конструктор приема 
     * @param buffer 
     */
    FwSyncTime(int controller,Date datelast, Date datenow)
    {
        this.controller=controller;
        this.datelast=datelast;
        this.datenow=datenow;
    }
    
    FwSyncTime(byte[] buffer,int controller)
    {
        this.controller=controller;
        datelast=new Date(FwUtil.ToLong(buffer, 0));
        datenow=new Date(FwUtil.ToLong(buffer, 8));
    }
    @Override
    public int toBuffer(byte[] buffer,int pos){
        FwUtil.LongToBuff(buffer, pos, getDatelast().getTime());
        FwUtil.LongToBuff(buffer, pos+8, getDatenow().getTime());
        return 16;
    }

    /**
     * @return the datelast
     */
    public Date getDatelast()
    {
        return datelast;
    }

    /**
     * @return the datenow
     */
    public Date getDatenow()
    {
        return datenow;
    }

    /**
     * @return the controller
     */
    @Override
    public int getController()
    {
        return controller;
    }

    @Override
    public byte getFunctionCode()
    {
        return functioncode;
    }
    
}
