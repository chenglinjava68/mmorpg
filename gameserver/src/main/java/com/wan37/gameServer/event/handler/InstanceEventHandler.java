package com.wan37.gameServer.event.handler;

import com.wan37.gameServer.event.EventBus;
import com.wan37.gameServer.event.model.InstanceEvent;
import com.wan37.gameServer.game.mission.model.MissionCondition;
import com.wan37.gameServer.game.mission.model.MissionType;
import com.wan37.gameServer.game.mission.service.MissionService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author gonefuture  gonefuture@qq.com
 * time 2019/1/3 17:27
 * @version 1.00
 * Description: mmorpg
 */

@Component
public class InstanceEventHandler {

    {
        EventBus.subscribe(InstanceEvent.class,this::passInstance);
    }

    @Resource
    private MissionService missionService;


    private  void passInstance(InstanceEvent guildEvent) {
        missionService.checkMissionProgress(MissionType.INSTANCE,guildEvent.getPlayer(), MissionCondition.FIRST_ACHIEVEMENT);
    }
}
