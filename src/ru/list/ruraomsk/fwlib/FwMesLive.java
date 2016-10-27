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
class FwMesLive extends FwBaseMess
{
    private int controller;
    private int counter;
    private final byte functioncode=FwUtil.FP_CODE_64H;
//    FwMesLive(int controller){
//        this.controller=controller;
//        counter=0;
//    }
    FwMesLive(int controller,int counter){
        this.controller=controller;
        this.counter=counter;
    }
    FwMesLive(byte[] buffer,int controller){
        this.controller=controller;
        counter=FwUtil.ToShort(buffer, 0);
    }

    @Override
    public int toBuffer(byte[] buffer, int pos)
    {
        FwUtil.IntToBuff(buffer, pos, getCounter());
        return 2;
    }

    /**
     * @return the controller
     */
    @Override
    public int getController()
    {
        return controller;
    }

    /**
     * @return the counter
     */
    public int getCounter()
    {
        return counter;
    }

    /**
     * @param counter the counter to set
     */
    public void setCounter(int counter)
    {
        this.counter = counter;
    }

    @Override
    public byte getFunctionCode()
    {
        return functioncode;
    }
    
}
