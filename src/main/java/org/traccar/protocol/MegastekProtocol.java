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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.traccar.BaseProtocol;
import org.traccar.NetworkMessage;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.handler.OpenChannelHandler;
import org.traccar.model.Command;
import org.traccar.config.Config;

import javax.inject.Inject;

public class MegastekProtocol extends BaseProtocol {

    @Inject
    public MegastekProtocol(Config config) {
        setSupportedDataCommands(
            Command.TYPE_SET_CONNECTION,
            Command.TYPE_POSITION_STOP,
            Command.TYPE_POSITION_PERIODIC,
            Command.TYPE_SOS_NUMBER,
            Command.TYPE_SET_TIMEZONE,
            Command.TYPE_GET_DEVICE_STATUS,
            Command.TYPE_ALARM_VIBRATION,
            Command.TYPE_SILENCE_TIME,
            Command.TYPE_POSITION_SINGLE,
            Command.TYPE_FACTORY_RESET,
            Command.TYPE_REBOOT_DEVICE,
            Command.TYPE_ALARM_SPEED,
            Command.TYPE_MODE_POWER_SAVING,
            Command.TYPE_OUTPUT_CONTROL,
            Command.TYPE_CUSTOM
        );
        addServer(new TrackerServer(config, getName(), false) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new MegastekFrameDecoder());
                pipeline.addLast(new StringEncoder());
                pipeline.addLast(new StringDecoder());
                pipeline.addLast(new MegastekProtocolEncoder(MegastekProtocol.this));
                pipeline.addLast(new MegastekProtocolDecoder(MegastekProtocol.this));
                pipeline.addLast(new OpenChannelHandler(this) {
                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        super.channelActive(ctx);
                        ctx.writeAndFlush(new NetworkMessage("$GPRS,;!", ctx.channel().remoteAddress()));
                    }
                });
            }
        });
    }

}
