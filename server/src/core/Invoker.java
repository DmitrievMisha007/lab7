package core;

import commands.*;
import interfases.Command;

public class Invoker {
    public Command help;
    public Command info;
    public Command show;
    public Command add;
    public Command updateId;
    public Command removeById;
    public Command clear;
    public Command register;
    public Command addIfMax;
    public Command addIfMin;
    public Command history;
    public Command filterGreaterThanPrice;
    public Command printFieldAscendingPrice;
    public Command printFieldDescendingRefundable;

    /**
     * Метод инициализации класса
     */
    public void init(){
        help = new Help();
        info = new Info();
        show = new Show();
        add = new Add();
        updateId = new UpdateId();
        removeById = new RemoveById();
        clear = new Clear();
        register = new Register();
        addIfMax = new AddIfMax();
        addIfMin = new AddIfMin();
        history = new History();
        filterGreaterThanPrice = new FilterGreaterThanPrice();
        printFieldAscendingPrice = new PrintFieldAscendingPrice();
        printFieldDescendingRefundable = new PrintFieldDescendingRefundable();
    }
}
