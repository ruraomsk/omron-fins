/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

import java.util.Arrays;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class FwSetup extends FwBaseMess
{

    private byte functioncode = FwUtil.FP_CODE_34H;
    private int controller;
    private int nomer_file;
    private int nomer_mess;
    private int command;
    private int lenght = 0;
    private byte[] data_file;

public FwSetup(boolean master, int controller, int nomer_file, int moner_mess, int command)
    {
        if (master) {
            functioncode = FwUtil.FP_CODE_34H;
        }
        else {
            functioncode = FwUtil.FP_CODE_36H;
        }
        this.controller = controller;
        this.nomer_file = nomer_file;
        this.nomer_mess = moner_mess;
        this.command = command;
    }

public FwSetup(boolean master, int len, byte[] buffer, int controller)
    {
        if (master) {
            functioncode = FwUtil.FP_CODE_34H;
        }
        else {
            functioncode = FwUtil.FP_CODE_36H;
        }
        this.controller = controller;
        nomer_file = (int) buffer[0];
        nomer_mess = (int) buffer[1];
        command = (int) buffer[2];
        lenght=0;
        if (command == 1 || command == 3) {
            data_file = new byte[len - 3];
            lenght=data_file.length;
            System.arraycopy(buffer, 3, data_file, 0, lenght);
//            System.err.println("FwSetup"+Arrays.toString(data_file));
        }
        if (command == 5) {
            lenght = (int) buffer[3];
        }

    }
    public void setLenght(int lenght)
    {
        this.lenght = lenght;
    }

    public void setData(byte[] data_buf, int lenght)
    {
        data_file = new byte[lenght];
        this.lenght = lenght;
        System.arraycopy(data_buf, 0, data_file, 0, lenght);
    }

    @Override
    public int toBuffer(byte[] outbuf, int pos)
    {
        int len;
        outbuf[pos++] = (byte) (nomer_file & 0xFF);
        outbuf[pos++] = (byte) (nomer_mess & 0xFF);
        outbuf[pos++] = (byte) (command & 0xFF);
        len = 3;
        if (command == 1 || command == 3) {
            System.arraycopy(data_file, 0, outbuf, pos, lenght);
            len += lenght;
        }
        if (command == 5) {
            outbuf[pos++] = (byte) (lenght & 0xFF);
            len++;
        }
//            System.err.println("toBuffer"+Arrays.toString(data_file));
        return len;
    }

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

    public int getNomFile()
    {
        return nomer_file;
    }

    public int getNomer()
    {
        return nomer_mess;
    }

    public int getCmd()
    {
        return command;
    }

    public int getLenght()
    {
        return lenght;
    }

    public String getBuffer()
    {
        if(data_file==null)return "null";
        return new String(data_file,0,lenght);
    }

}
