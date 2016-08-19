/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

/**
 * Базовый класс для всех сообщений  
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
abstract class FwBaseMess
{
/**
 * 
 * @return Возвращает номер контроллера
 */
    abstract public int getController();
    /**
     * Упаковывает сообщение в буфер
     * @param buffer
     * @param pos
     * @return  длину упакованного сообщения
     */
    abstract public int toBuffer(byte[] buffer,int pos);
/**
 * 
 * @return Возвращает номер функции сообщения
 */
    abstract public byte getFunctionCode();
}
