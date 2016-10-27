/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Класс для хранения значений переменных сообщений 1h.
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
class FwInfo extends FwBaseMess
{

    private final byte functioncode = FwUtil.FP_CODE_INFO;
    private ArrayList<FwOneReg> datas = new ArrayList(FwUtil.VALUE_UIDS);
    private int nomer;
    private int pos;
    private int controller;
    private FwRegisters tableDecode;
    /**
     * Максимальный колличество переменных для записи в буфер
     */
    static private final int MAX_SIZE = 100;

    /**
     * Конструктор для подготовки передачи на сервер
     *
     * @param controller - номер контроллера
     * @param tableDecode - таблица имен и типов переменных
     * @param nomer - номер создаваемого сообщения
     */
    public FwInfo(int controller, int nomer)
    {
        this.controller = controller;
        this.nomer = nomer;
    }

    /**
     * Выгрузка сообщения в буфер с указанной позиции
     *
     * @param buffer
     * @param tpos
     * @return длина выгруженного сообщения в байтах
     */
    @Override
    public int toBuffer(byte[] buffer, int tpos)
    {
        this.pos = tpos;
        FwOneReg oreg = datas.get(0);
        Date firstdate = oreg.getDate();
        FwUtil.LongToBuff(buffer, pos, firstdate.getTime());
        pos += 8;
        FwUtil.IntToBuff(buffer, pos, nomer);
        pos += 4;
        short kolvo = 1;
        int i;
        setValue(buffer, oreg);

        for (i = 1; i < datas.size(); i++) {
            oreg = datas.get(i);
            if (oreg.getDate() != firstdate) {
                break;
            }
            kolvo++;
            setValue(buffer, oreg);
        }
        if (kolvo == 0) {
            return 0;
        }
        FwUtil.IntToBuff(buffer, tpos + 10, kolvo);
        if (i == datas.size()) {
            return pos - tpos;
        }
        do {
            oreg = datas.get(i);
            int temp = (int) (oreg.getDate().getTime() - firstdate.getTime());
            FwUtil.IntToBuff(buffer, pos, temp);
            pos += 2;
            int spos = pos;
            pos += 2;
            Date tfirstdate = oreg.getDate();
            kolvo = 0;
            do {
                setValue(buffer, oreg);
                kolvo++;
                i++;
                if (i >= datas.size()) {
                    break;
                }
                oreg = (FwOneReg) datas.get(i);
                if (tfirstdate != oreg.getDate()) {
                    break;
                }
            }
            while (true);
            FwUtil.IntToBuff(buffer, spos, kolvo);
            if (i >= datas.size()) {
                break;
            }
        }
        while (true);
        return pos - tpos;
    }

    private void setValue(byte[] buffer, FwOneReg oreg)
    {
        FwUtil.IntToBuff(buffer, pos, oreg.getReg().getuId());
        pos += 2;
        oreg.setBuffer(buffer, pos);
        pos += oreg.getReg().getLen();
        buffer[pos++] = oreg.getGood();
    }

    /**
     * Конструктор для чтения принятого от устройства сообщения
     *
     * @param len длина буфура сообщения
     * @param buffer собственно буфер
     * @param controller номер контроллера
     * @param tableDecode таблица имен и типов переменных
     * @throws IOException выбрасывается если в структуре сообщения ошибки
     */
    public FwInfo(int len, byte[] buffer, int controller, FwRegisters tableDecode)
    {
        Date date = null, ndate = null;
        date = new Date(FwUtil.ToLong(buffer, 0));
        nomer = FwUtil.ToShort(buffer, 8);
        pos = 12;
        this.controller = controller;
        this.tableDecode = tableDecode;
        FwUtil.textError = "";
        //for (FwRegister reg : tableDecode.getCollection())
        //{
        //    if (FwUtil.FP_DEBUG)
        //    {
        //        FwUtil.textError += reg.toString() + "/ ";
        //    }
        //}

        int kolvo = FwUtil.ToShort(buffer, 10);
        for (int i = 0; i < kolvo; i++) {
            FwOneReg temp = getValue(buffer);
            temp.setDate(date);
            datas.add(temp);
        }
        while (pos < len) {
            int addms = FwUtil.ToShort(buffer, pos);
            pos += 2;
            kolvo = FwUtil.ToShort(buffer, pos);
            pos += 2;
            ndate = new Date(date.getTime() + addms);
            for (int i = 0; i < kolvo; i++) {
                FwOneReg temp = getValue(buffer);
                temp.setDate(ndate);
                datas.add(temp);
            }

        }

    }

    private FwOneReg getValue(byte[] buffer)
    {
        int uId = FwUtil.ToShort(buffer, pos);
        pos += 2;
        FwOneReg temp = new FwOneReg();
        temp.setReg(tableDecode.getRegister(controller, uId));
        if (temp.getReg() == null) {
            FwUtil.textError = "not found " + Integer.toString(controller) + ":" + Integer.toString(uId);
            return null;

            //throw new IOException("not found " + Integer.toString((int) controller) + ":" + Integer.toString(uId));
        }
        temp.getBuffer(buffer, pos);
        pos += temp.getReg().getLen();
        temp.setGood(buffer[pos++]);
        return temp;
    }

    /**
     * @return the nomer
     */
    public int getNomer()
    {
        return nomer;
    }

    public int getSize()
    {
        return datas.size();
    }

    public FwOneReg getOneReg(int idx)
    {
        return datas.get(idx);
    }

    /**
     * Добавляет в очередь вывода переменную
     *
     * @param value в формате OneReg значение переменной на вывод
     * @return false если места уже нет . Соответственно переменная не
     * записывается true -если все ок
     */
    public boolean addOneReg(FwOneReg value)
    {
        if (isFull()) {
            return false;
        }
        datas.add(value);
        return true;
    }

    /**
     *
     * @return true is full
     */
    public boolean isFull()
    {
        if (datas.size() > MAX_SIZE) {
            return true;
        }
        else {
            return false;
        }
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

}
