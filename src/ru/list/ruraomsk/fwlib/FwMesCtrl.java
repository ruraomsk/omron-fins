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
public class FwMesCtrl extends FwBaseMess
{

    private int controller;
    private byte commandcode;
    private byte good;
    private parameter[] params = new parameter[7];
    private final byte functioncode = FwUtil.FP_CODE_10H;

    /**
     * Создание сообщения
     *
     * @param commandcode
     */
public FwMesCtrl(int controller, int commandcode)
    {
        this.controller = controller;
        this.commandcode = (byte) (commandcode & 0xff);
        this.good = FwUtil.FP_DATA_GOOD;
        for (int i = 0; i < 7; i++) {
            params[i] = new parameter();
        }
    }

    FwMesCtrl(byte[] buffer, int controller)
    {
        this.controller = controller;
        int pos = 0;
        commandcode = buffer[pos++];
        good = buffer[pos++];
        for (int i = 0; i < 7; i++) {
            params[i] = new parameter();
            setPar(i, FwUtil.ToShort(buffer, pos));
            pos += 2;
            setGood(i, buffer[pos++]);
        }
    }

    public void setPar(int idx, int value)
    {
        if (idx >= params.length || idx < 0) {
            return;
        }
        params[idx].par = value;
    }

    public int getPar(int idx)
    {
        if (idx >= params.length || idx < 0) {
            return 0;
        }
        return params[idx].par;
    }

    public byte getGood(int idx)
    {
        if (idx >= params.length || idx < 0) {
            return 0;
        }
        return params[idx].good;
    }

    public void setGood(int idx, byte good)
    {
        if (idx >= params.length || idx < 0) {
            return;
        }
        params[idx].good = good;
    }

    /**
     * возможное число параметров
     *
     * @return
     */
    public int getSize()
    {
        return params.length;
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
     * @return the commandcode
     */
    public byte getCommandcode()
    {
        return commandcode;
    }

    class parameter
    {

        int par = 10;
        byte good = FwUtil.FP_DATA_GOOD;

    }

    @Override
    public int toBuffer(byte[] buffer, int pos)
    {
        int tpos = pos;
        buffer[pos++] = getCommandcode();
        buffer[pos++] = good;
        for (int i = 0; i < params.length; i++) {
            FwUtil.IntToBuff(buffer, pos, getPar(i));
            pos += 2;
            buffer[pos++] = getGood(i);
        }
        return pos - tpos;
    }

    @Override
    public byte getFunctionCode()
    {
        return functioncode;
    }

}
