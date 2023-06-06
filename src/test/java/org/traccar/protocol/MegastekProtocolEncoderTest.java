package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MegastekProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeSimple() throws Exception {

        var encoder = inject(new MegastekProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_SINGLE);

        assertEquals("$GPRS,123456789012345;W052;!", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeTimezone() throws Exception {

        var encoder = inject(new MegastekProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SET_TIMEZONE);
        command.set(Command.KEY_TIMEZONE, "Europe/Berlin");

        assertEquals("$GPRS,123456789012345;W020,60;!", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeSilenceTime() throws Exception {

        var encoder = inject(new MegastekProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_SILENCE_TIME);
        command.set(Command.KEY_DATA, "20:00-08:00");

        assertEquals("$GPRS,123456789012345;W038,08:00,20:00;!", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeFrequency() throws Exception {

        var encoder = inject(new MegastekProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_POSITION_PERIODIC);
        command.set(Command.KEY_FREQUENCY, 75);

        assertEquals("$GPRS,123456789012345;W005,2;!", encoder.encodeCommand(command));

        command.set(Command.KEY_FREQUENCY, 0);
        assertEquals("$GPRS,123456789012345;W005,1;!", encoder.encodeCommand(command));

    }

    @Test
    public void testEncodeCustom() throws Exception {

        var encoder = inject(new MegastekProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "$SMS,000000;R005;R029;!");

        assertEquals("$GPRS,123456789012345;R005;R029;!", encoder.encodeCommand(command));

        command.set(Command.KEY_DATA, "$GPRS,123456789012345;R075,FOO;!");
        assertEquals("$GPRS,123456789012345;R075,FOO;!", encoder.encodeCommand(command));

        command.set(Command.KEY_DATA, "W005,1;R029;");
        assertEquals("$GPRS,123456789012345;W005,1;R029;!", encoder.encodeCommand(command));

    }
}
