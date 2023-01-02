package lora;

import org.apache.commons.lang3.StringUtils;

public class Test {

    public static void main(String[] args) {


        System.out.println(String.format("%04X", 250));

        int address = Integer.parseInt(StringUtils.substringAfter("AT+ADDR=CCAA", "="), 16);
        System.out.println(address);
    }

}
