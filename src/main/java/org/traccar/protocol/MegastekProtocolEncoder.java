/*
 * Copyright 2021 Oliver Schramm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import java.util.TimeZone;
import org.traccar.model.Command;
import org.traccar.Protocol;
import org.traccar.StringProtocolEncoder;

public class MegastekProtocolEncoder extends StringProtocolEncoder implements StringProtocolEncoder.ValueFormatter {

    public MegastekProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    public String formatValue(String key, Object value) {

        switch (key) {
            case Command.KEY_FREQUENCY:
                int seconds = (int) value;
                // Round with bias to favour smaller intervals to bigger ones.
                Integer interval = Math.round((seconds - 5) / 30.0f);
                if (interval < 1) {
                    interval = 1;
                }
                if (interval > 65535) {
                    interval = 65535;
                }
                return interval.toString();
            case Command.KEY_TIMEZONE:
                Integer timezone = TimeZone.getTimeZone(value.toString()).getRawOffset() / 60000;
                return timezone.toString();
            default:
                return null;
        }
    }

    @Override
    protected Object encodeCommand(Command command) {

        switch (command.getType()) {
            case Command.TYPE_SET_CONNECTION:
                return formatCommand(command, "$GPRS,%s;W003,%s,%s;!",
                    Command.KEY_UNIQUE_ID,
                    Command.KEY_SERVER,
                    Command.KEY_PORT);
            case Command.TYPE_POSITION_STOP:
                return formatCommand(command, "$GPRS,%s;W005,0;!", Command.KEY_UNIQUE_ID);
            case Command.TYPE_POSITION_PERIODIC:
                return formatCommand(command, "$GPRS,%s;W005,%s;!", this, Command.KEY_UNIQUE_ID, Command.KEY_FREQUENCY);
            case Command.TYPE_SOS_NUMBER:
                return formatCommand(command, "$GPRS,%s;W010,%s,%s,001;!",
                    Command.KEY_UNIQUE_ID,
                    Command.KEY_INDEX,
                    Command.KEY_PHONE);
            case Command.TYPE_SET_TIMEZONE:
                return formatCommand(command, "$GPRS,%s;W020,%s;!", this, Command.KEY_UNIQUE_ID, Command.KEY_TIMEZONE);
            case Command.TYPE_GET_VERSION:
            case Command.TYPE_GET_DEVICE_STATUS:
                return formatCommand(command, "$GPRS,%s;R029;!", Command.KEY_UNIQUE_ID);
            case Command.TYPE_ALARM_VIBRATION:
                int times = command.getInteger(Command.KEY_DATA);
                if (times > 255) {
                    times = 255;
                }
                return formatCommand(command, "$GPRS,%s;W036," + times + ";!", Command.KEY_UNIQUE_ID);
            case Command.TYPE_SILENCE_TIME:
                String data = command.getString(Command.KEY_DATA);
                String[] time = data.split("-");
                if (time.length != 2) {
                    return null;
                }
                return formatCommand(command, "$GPRS,%s;W038," + time[1] + "," + time[0] + ";!", Command.KEY_UNIQUE_ID);
            case Command.TYPE_POSITION_SINGLE:
                return formatCommand(command, "$GPRS,%s;W052;!", Command.KEY_UNIQUE_ID);
            case Command.TYPE_FACTORY_RESET:
                return formatCommand(command, "$GPRS,%s;C099;!", Command.KEY_UNIQUE_ID);
            case Command.TYPE_REBOOT_DEVICE:
                return formatCommand(command, "$GPRS,%s;W100;!", Command.KEY_UNIQUE_ID);
            case Command.TYPE_CUSTOM:
                String customCommand = command.getString(Command.KEY_DATA);
                boolean isSmsCommand = customCommand.startsWith("$SMS");

                if (customCommand.startsWith("$")
                    || (!customCommand.startsWith("W")
                        && !customCommand.startsWith("R")
                        && !customCommand.startsWith("C"))
                ) {
                    // If it's correctly entered, we shouldn't doctor with it and it's good to go.
                    if (!isSmsCommand) {
                        return customCommand;
                    }
                }
                if (isSmsCommand) {
                    // If someone entered the SMS format, sending via GPRS won't work, so we replace it.
                    customCommand = customCommand.substring(customCommand.indexOf(";") + 1);
                }
                if (!customCommand.endsWith(";!")) {
                    if (customCommand.endsWith(";")) {
                        customCommand += "!";
                    } else {
                        customCommand += ";!";
                    }
                }
                return formatCommand(command, "$GPRS,%s;" + customCommand, Command.KEY_UNIQUE_ID);
            default:
                return null;
        }
    }
}
