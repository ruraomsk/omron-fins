/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.monitor;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import ru.list.ruraomsk.fwlib.FwMasterDevice;
import ru.list.ruraomsk.fwlib.FwRegister;
import ru.list.ruraomsk.fwlib.FwRegisters;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class MsTable extends AbstractTableModel
{
        FwRegisters tableDecode;
        FwMasterDevice md;

    public MsTable(FwRegisters tableDecode, FwMasterDevice md)
    {
        this.tableDecode=tableDecode;
        this.md=md;
    }
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return false;
        }

        @Override
        public String getColumnName(int column)
        {
            switch (column)
            {
                case 0:
                    return "Имя";
                case 1:
                    return "Тип";
                case 2:
                    return "uId";
                case 3:
                    return "Значение";
            }
            return "Bad Column";
        }


        @Override
        public int getRowCount()
        {
            int count=0;
            for (FwRegister reg : tableDecode.getCollection())
            {
                if(md.getController()==reg.getController()) count++;
            }

            return count;
        }

        @Override
        public int getColumnCount()
        {
            return 4;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            int idx = 0;
            for (FwRegister reg : tableDecode.getCollection())
            {
                if(md.getController()!=reg.getController()) continue;
                if (rowIndex == idx++)
                {
                    switch (columnIndex)
                    {
                        case 0:
                            return Monitor.globalName.get(reg.getKey());
                        case 1:
                            return reg.isInfo() ? "Информация" : "Диагностика";
                        case 2:
                            return reg.getuId();
                        case 3:
                            return reg.isInfo() ? md.getValue(reg.getuId()) : md.getCode(reg.getuId());
                    }
                    return null;
                }

            }
            return null;
        }

    
}
