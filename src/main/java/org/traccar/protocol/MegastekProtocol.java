/*
 * Copyright 2015 - 2018 Anton Tananaev (anton@traccar.org)
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

import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.model.Command;

public class MegastekProtocol extends BaseProtocol {

    public MegastekProtocol() {
        setSupportedDataCommands(
            Command.TYPE_SET_CONNECTION,
            Command.TYPE_POSITION_STOP,
            Command.TYPE_POSITION_PERIODIC,
            Command.TYPE_SOS_NUMBER,
            Command.TYPE_SET_TIMEZONE,
            Command.TYPE_GET_VERSION,
            Command.TYPE_GET_DEVICE_STATUS,
            Command.TYPE_ALARM_VIBRATION,
            Command.TYPE_SILENCE_TIME,
            Command.TYPE_POSITION_SINGLE,
            Command.TYPE_FACTORY_RESET,
            Command.TYPE_REBOOT_DEVICE,
            Command.TYPE_CUSTOM
        );
        addServer(new TrackerServer(false, getName()) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline) {
                pipeline.addLast(new MegastekFrameDecoder());
                pipeline.addLast(new StringEncoder());
                pipeline.addLast(new StringDecoder());
                pipeline.addLast(new MegastekProtocolEncoder(MegastekProtocol.this));
                pipeline.addLast(new MegastekProtocolDecoder(MegastekProtocol.this));
            }
        });
    }

}
