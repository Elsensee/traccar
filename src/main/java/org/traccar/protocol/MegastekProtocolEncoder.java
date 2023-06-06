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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.traccar.config.Keys;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.model.Command;
import org.traccar.Protocol;
import org.traccar.StringProtocolEncoder;

public class MegastekProtocolEncoder extends StringProtocolEncoder {

    private VehicleProtocolFormatter vehicleFormatter;
    private PersonalProtocolFormatter personalFormatter;

    private Pattern vehicleSmsPattern;

    public MegastekProtocolEncoder(Protocol protocol) {
        super(protocol);

        vehicleFormatter = new VehicleProtocolFormatter();
        personalFormatter = new PersonalProtocolFormatter();

        vehicleSmsPattern = Pattern.compile("^(?:M\\d{6},)?(\\d{2})(?:,(.*))?");
    }

    private String calculateChecksum(String text) {
        int startIndex = text.indexOf('$') + 1;

        int endIndex = text.lastIndexOf(';');
        if (endIndex < 0) {
            endIndex = text.length();
        }

        char checksum = 0;
        for (int i = startIndex; i < endIndex; i++) {
            checksum ^= (text.charAt(i));
        }

        return String.format("%02X", (int) checksum);
    }

    @Override
    protected Object encodeCommand(Command command) {

        boolean alternative = AttributeUtil.lookup(getCacheManager(), Keys.PROTOCOL_ALTERNATIVE.withPrefix(getProtocolName()), command.getDeviceId());

        if (alternative) {
            switch (command.getType()) {
                case Command.TYPE_SET_CONNECTION:
                    return formatCommand(command, "$GPRS,%s;W003,%s,%s;!",
                        Command.KEY_UNIQUE_ID,
                        Command.KEY_SERVER,
                        Command.KEY_PORT);
                case Command.TYPE_POSITION_STOP:
                    return formatCommand(command, "$GPRS,%s;W005,0;!", Command.KEY_UNIQUE_ID);
                case Command.TYPE_POSITION_PERIODIC:
                    return formatCommand(command, "$GPRS,%s;W005,%s;!", this.personalFormatter,
                        Command.KEY_UNIQUE_ID,
                        Command.KEY_FREQUENCY);
                case Command.TYPE_SOS_NUMBER:
                    return formatCommand(command, "$GPRS,%s;W010,%s,%s,001;!",
                        Command.KEY_UNIQUE_ID,
                        Command.KEY_INDEX,
                        Command.KEY_PHONE);
                case Command.TYPE_SET_TIMEZONE:
                    return formatCommand(command, "$GPRS,%s;W020,%s;!", this.personalFormatter,
                    Command.KEY_UNIQUE_ID,
                    Command.KEY_TIMEZONE);
                case Command.TYPE_GET_DEVICE_STATUS:
                    return formatCommand(command, "$GPRS,%s;R029;!", Command.KEY_UNIQUE_ID);
                case Command.TYPE_ALARM_VIBRATION:
                    int times = command.getInteger(Command.KEY_DATA);

                    if (times < 0) {
                        command.set(Command.KEY_DATA, 0);
                    }
                    if (times > 255) {
                        command.set(Command.KEY_DATA, 255);
                    }
                    return formatCommand(command, "$GPRS,%s;W036,%s;!", Command.KEY_UNIQUE_ID, Command.KEY_DATA);
                case Command.TYPE_SILENCE_TIME:
                    String data = command.getString(Command.KEY_DATA);
                    String[] time = data.split("-");
                    if (time.length != 2) {
                        return null; // TODO throw
                    }
                    for (int i = 0; i < time.length; i++) {
                        if (!time[i].contains(":")) {
                            time[i] += ":00";
                        }
                    }

                    return formatCommand(command, "$GPRS,%s;W038," + time[1] + "," + time[0] + ";!",
                        Command.KEY_UNIQUE_ID);
                case Command.TYPE_POSITION_SINGLE:
                    return formatCommand(command, "$GPRS,%s;W052;!", Command.KEY_UNIQUE_ID);
                case Command.TYPE_FACTORY_RESET:
                    return formatCommand(command, "$GPRS,%s;C099;!", Command.KEY_UNIQUE_ID);
                case Command.TYPE_REBOOT_DEVICE:
                    return formatCommand(command, "$GPRS,%s;W100;!", Command.KEY_UNIQUE_ID);
                case Command.TYPE_CUSTOM:
                    String customCommand = command.getString(Command.KEY_DATA);

                    if (!customCommand.toUpperCase().matches("^[WRC]\\d{3}(?:[,;].*|$)")) {
                        if (!customCommand.startsWith("$SMS")) {
                            // If it's correctly entered, we shouldn't doctor with it and it's good to go.
                            return customCommand;
                        } else {
                            // If someone entered the SMS format, sending via GPRS won't work, so we replace it.
                            customCommand = customCommand.substring(customCommand.indexOf(";") + 1);
                        }
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
                    break;
            }
        } else {
            switch (command.getType()) {
                case Command.TYPE_POSITION_PERIODIC:
                    return formatWithChecksum(command, "$,%s,0013,%s;", this.vehicleFormatter,
                        Command.KEY_UNIQUE_ID,
                        Command.KEY_FREQUENCY);
                case Command.TYPE_POSITION_SINGLE:
                    return formatWithChecksum(command, "$,%s,0015;", null, Command.KEY_UNIQUE_ID);
                case Command.TYPE_SOS_NUMBER:
                    return formatWithChecksum(command, "$,%s,0020,%s,%s,10000000000;", null,
                        Command.KEY_UNIQUE_ID,
                        Command.KEY_INDEX,
                        Command.KEY_PHONE);
                case Command.TYPE_ALARM_SPEED:
                    int alarmData = command.getInteger(Command.KEY_DATA);
                    if (alarmData < 0) {
                        command.set(Command.KEY_DATA, 0);
                    } else if (alarmData > 200) {
                        command.set(Command.KEY_DATA, 200);
                    }
                    return formatWithChecksum(command, "$,%s,0030,%s;", null, Command.KEY_UNIQUE_ID, Command.KEY_DATA);
                case Command.TYPE_MODE_POWER_SAVING:
                    return formatWithChecksum(command, "$,%s,0031,%s;", this.vehicleFormatter,
                        Command.KEY_UNIQUE_ID,
                        Command.KEY_ENABLE);
                case Command.TYPE_ALARM_VIBRATION:
                    int vibrationData = command.getInteger(Command.KEY_DATA);
                    if (vibrationData < 0) {
                        command.set(Command.KEY_DATA, 0);
                    } else if (vibrationData > 1) {
                        command.set(Command.KEY_DATA, 1);
                    }
                    return formatWithChecksum(command, "$,%s,0032,%s;", null, Command.KEY_UNIQUE_ID, Command.KEY_DATA);
                case Command.TYPE_GET_DEVICE_STATUS:
                    return formatWithChecksum(command, "$,%s,0040;", null, Command.KEY_UNIQUE_ID);
                case Command.TYPE_OUTPUT_CONTROL:
                    int outputData = command.getInteger(Command.KEY_DATA);
                    if (outputData < 0) {
                        command.set(Command.KEY_DATA, 0);
                    } else if (outputData > 1) {
                        command.set(Command.KEY_DATA, 1);
                    }
                    return formatWithChecksum(command, "$,%s,0050,%s,%s;", null, Command.KEY_UNIQUE_ID,
                        Command.KEY_INDEX,
                        Command.KEY_DATA);
                case Command.TYPE_CUSTOM:
                    String customCommand = command.getString(Command.KEY_DATA);

                    String resultCommand;

                    if (!customCommand.matches("^\\d{4}(?:[,].*|;|$)")) {
                        Matcher smsMatcher = vehicleSmsPattern.matcher(customCommand);
                        String smsCommand = "";
                        String smsArguments = "";

                        if (smsMatcher.matches()) {
                            smsCommand = smsMatcher.group(1);
                            smsArguments = smsMatcher.group(2);
                        }

                        switch (smsCommand) {
                            case "01": // authorized number
                                resultCommand = "0020,";
                                break;
                            case "02": // single position
                                resultCommand = "0015,";
                                break;
                            case "03": // track regularly via sms
                                resultCommand = "0023,";
                                break;
                            case "05": // over speed alarm
                                resultCommand = "0030,";
                                break;
                            case "10": // sms format
                                resultCommand = "0024,";
                                break;
                            case "25": // GPRS interval
                                resultCommand = "0013,";
                                break;
                            case "26": // gprs distance
                                resultCommand = "0016,";
                                break;
                            case "27": // gps corner
                                resultCommand = "0017,";
                                break;
                            case "32": // geo fence
                                resultCommand = "0033,";
                                break;
                            case "50": // output control
                                resultCommand = "0050,";
                                break;
                            default:
                                // 21 => TCP/UDP
                                // 22 => Set ID
                                // 24 => IP and PORT
                                if (!customCommand.endsWith("\r\n")) {
                                    customCommand += "\r\n";
                                }
                                return customCommand;
                        }

                        resultCommand += smsArguments;
                    } else {
                        resultCommand = customCommand;
                    }

                    if (resultCommand.endsWith("\r\n")) {
                        return resultCommand;
                    }
                    if (!resultCommand.endsWith(";")) {
                        resultCommand += ";";
                    }

                    return formatWithChecksum(command, "$,%s," + resultCommand, null, Command.KEY_UNIQUE_ID);
                default:
                    break;
            }
        }

        throw new RuntimeException("Command " + command.getType() + " is not supported for this device via GPRS");
    }

    private String formatWithChecksum(Command command, String format, ValueFormatter valueFormatter, String... keys) {
        var result = formatCommand(command, format, valueFormatter, keys);

        var checksum = calculateChecksum(result);
        return result + checksum + "\r\n";
    }

    private class VehicleProtocolFormatter implements ValueFormatter {

        @Override
        public String formatValue(String key, Object value) {

            switch (key) {
                case Command.KEY_FREQUENCY:
                    Integer seconds = (Integer) value;

                    if (seconds < 5) {
                        seconds = 5;
                    }
                    if (seconds > 65535) {
                        seconds = 65535;
                    }
                    return seconds.toString();
                case Command.KEY_ENABLE:
                    Boolean enable = (Boolean) value;

                    return enable ? "1" : "0";
                default:
                    return null;
            }
        }
    }

    private class PersonalProtocolFormatter implements ValueFormatter {

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
    }
}
