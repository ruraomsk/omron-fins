/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

import java.io.IOException;
import java.util.ArrayList;

/** 
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
class FwDiag extends FwBaseMess
{
    private final byte functioncode=FwUtil.FP_CODE_30H;
    private String UPCMessage;
    private byte[] Skp=new byte[3];
    private ArrayList<Integer> datas=new ArrayList(FwUtil.VALUE_UIDS);
    private int controller;
    private FwRegisters tableDecode;
/**
 * Конструктор для отправки сообщений
 * @param controller - номер контроллера
 * @param tableDecode - таблица имен 
 */
    FwDiag(int controller,FwRegisters tableDecode){
        this.controller=controller;
        this.tableDecode=tableDecode;
        UPCMessage="Nothing";
    }
    /**
     * Конструктор для приема сообщения
     * @param len - длина буфера с сообщением
     * @param buffer - собственно буфер куда транспорт выложил сообщение
     * @param controller - номер контроллера
     * @param tableDecode - таблица имен
     * @throws IOException 
     */
    FwDiag(int len, byte[] buffer, int controller, FwRegisters tableDecode)
    {
        this.controller=controller;
        this.tableDecode=tableDecode;
        int kolvo;
        int pos=0;
        if((len-47)<0) kolvo=len/2;
        else kolvo=(len-47)/2;
        for (int i = 0; i < kolvo; i++)
        {
            datas.add(FwUtil.ToShort(buffer, pos));
            pos+=2;
        }
        if((len-47)>0){
            System.arraycopy(buffer, pos, Skp, 0, 3);
            pos+=3;
            byte[] bb=new byte[44];
            System.arraycopy(buffer, pos, bb, 0, 44);
            UPCMessage=new String(bb);
        }
    }
    /**
     * Загружает в сообщение сотояние одной переменной диагностики
     * Не забываем что uId и код состояния обрезаются до одного байта
     * @param uId
     * @param code 
     */
    public void setOneDiag(int uId,int code){
        datas.add((uId<<8) | (code&0xff));
    }
  
    public int getDiagCode(int idx){
        return datas.get(idx)&0xff;
    }
    public int getDiagUId(int idx){
        return (datas.get(idx)>>8)&0xff;
    }
 
    @Override
    public int toBuffer(byte[] outbuf, int pos)
    {
        int tpos=pos;
        for (int j = 0; j < datas.size(); j++)
        {
            FwUtil.IntToBuff(outbuf, pos,datas.get(j) );
            pos+=2;
        }
        System.arraycopy(getSkp(), 0, outbuf, pos, 3);
        pos+=3;
        byte[] bb=getUPCMessage().getBytes();
        for (int i = 0; i < 44; i++)
        {
            outbuf[pos++]=(byte) ((i<bb.length)?bb[i]:0); 
        }
        return pos-tpos;
  
    }
    @Override
    public int getController()
    {
        return controller;    }
     @Override
    public byte getFunctionCode()
    {
        return functioncode;
    }

    /**
     * @return the UPCMessage
     */
    public String getUPCMessage()
    {
        return UPCMessage;
    }

    /**
     * @param UPCMessage the UPCMessage to set
     */
    public void setUPCMessage(String UPCMessage)
    {
        this.UPCMessage = UPCMessage;
    }

    /**
     * @return the Skp
     */
    public byte[] getSkp()
    {
        return Skp;
    }

    /**
     * @param Skp the Skp to set
     */
    public void setSkp(byte[] Skp)
    {
        System.arraycopy(Skp,0,this.Skp,0,3);
    }
    public int getSize(){
        return datas.size();
    }
}
