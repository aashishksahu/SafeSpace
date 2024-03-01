package org.privacymatters.safespace.lib.utils;

import static java.lang.Math.abs;

import java.math.BigInteger;
import java.util.ArrayList;

public class MergeSort {

    public static ArrayList<BigInteger> mergeSort(
            ArrayList<BigInteger> items,
            Integer startIndex,
            Integer stopIndex
    ) {
        int listLength = items.size();

        int start = (startIndex != null) ? startIndex : 0;

        int stop;

        if (stopIndex != null) {
            if (stopIndex % 2 == 0) {
                stop = stopIndex / 2 - 1;
            } else {
                stop = stopIndex / 2;
            }
        } else {
            if (listLength % 2 == 0) {
                stop = listLength / 2 - 1;
            } else {
                stop = listLength / 2;
            }
        }

        if(abs(start - stop) >= 1){
            mergeSort(items, start, stop);
        }

        if (abs(start - stop) == 1) {
            if (items.get(start).compareTo(items.get(stop)) > 0) {
                BigInteger temp = items.get(start);
                items.set(start, items.get(stop));
                items.set(stop, temp);
            }
        }

        if(abs(stop - listLength - 1) >= 1){
            mergeSort(items, stop , listLength - 1);
        }
        return items;
    }

    public static void main(String[] args) {

        ArrayList<BigInteger> list = new ArrayList<>();
        list.add(new BigInteger("12345"));
        list.add(new BigInteger("1234"));
        list.add(new BigInteger("123"));

        System.out.println(list);
        System.out.println(mergeSort(list, null, null));

    }
}
