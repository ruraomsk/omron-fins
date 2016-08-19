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
class FwSetup extends FwBaseMess
{
    private final byte functioncode=FwUtil.FP_CODE_34H;

    FwSetup(byte[] buffer,int controller)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int toBuffer(byte[] outbuf, int i)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getController()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public byte getFunctionCode()
    {
        return functioncode;
    }

}
