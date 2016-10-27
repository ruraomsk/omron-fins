/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

import java.util.Collection;
import java.util.HashMap;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class FwRegisters
{

    private HashMap<Long, FwRegister> dc = null;

    public FwRegisters()
    {
        dc = new HashMap(FwUtil.VALUE_UIDS);
    }

    public FwRegisters(int values)
    {
        dc = new HashMap(values);
    }

    public synchronized void add(FwRegister reg)
    {
        dc.put(reg.getKey(), reg);
    }

    public FwRegister getRegister(int controller, int uId)
    {
        long key = FwRegister.makeKey(controller, uId, true);
        return dc.get(key);
    }

    public FwRegister getRegisterDiag(int controller, int uId)
    {
        long key = FwRegister.makeKey(controller, uId, false);
        return dc.get(key);
    }

    public boolean isEmpty()
    {
        return dc.isEmpty();
    }

    public int getSize()
    {
        return dc.size();
    }

    public Collection<FwRegister> getCollection()
    {
        return dc.values();
    }
}
