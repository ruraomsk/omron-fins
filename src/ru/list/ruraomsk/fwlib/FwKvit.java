/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

/** 
 * Класс обработчик квитанций
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class FwKvit extends FwBaseMess
{
    private int controller;
    private byte[] mess=new byte[4];
    private final byte functioncode=FwUtil.FP_CODE_35H;
    /**
     * Чтение квитанции из буфера
     * @param buffer 
     */
    FwKvit(byte[] buffer,int controller)
    {
        this.controller=controller;
        System.arraycopy(buffer, 0, mess, 0, mess.length);
    }
/**
 * Создание пустой квитанции
 */
    public FwKvit(int controller)
    {
        this.controller=controller;
        for (int i = 0; i < mess.length; i++)
        {
            mess[i]=0;
        }
    }
    
    public byte getNomFile(){ 
        return mess[0];
    }
    public void setNomFile(int nom_file){ 
        mess[0]=(byte) (nom_file&0xf);
    }
    public byte getNomer(){ 
        return mess[1];
    }
    public void setNomer(int nomer){ 
        mess[1]=(byte) (nomer&0xf);
    }
    public byte getCmd(){ 
        return mess[2];
    }
    public void setCmd(int cmd){ 
        mess[2]=(byte) (cmd&0xf);
    }
    public byte getRezult(){ 
        return mess[3];
    }
    public void setRezult(int rezult){ 
        mess[3]=(byte) (rezult&0xf);
    }
    /**
     * Выгрузка квинтанции в буфер с указанной позиции
     * @param buffer
     * @param pos
     * @return длина выгруженного сообщения в байтах
     */
    @Override
    public int toBuffer(byte[] buffer,int pos)
    {
        System.arraycopy(mess, 0, buffer, pos, mess.length);
        return mess.length;
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