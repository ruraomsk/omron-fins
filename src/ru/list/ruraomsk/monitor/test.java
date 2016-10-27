/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.monitor;

import ru.list.ruraomsk.fwlib.FwUtil;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class test
{
    static byte[] buffer =new byte[10];
    
    static void fill(int b){
    int pos=0;
    while (pos<buffer.length)
    {
            FwUtil.IntToBuff(buffer, pos, b);
            pos+=2;
        }
        for (int i = 0; i < buffer.length; i++)
        {
            System.out.print(Integer.toString(buffer[i], 16)+" ");
        }
        System.out.println();
}
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        for (int i = 0; i < 10; i++)
        {
            fill(i);
            System.out.println("fill="+Integer.toString(i)+" CRC="+Integer.toString(FwUtil.Crc(buffer, 0, buffer.length)));
        }
    }
    
}
