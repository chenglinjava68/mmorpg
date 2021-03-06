package com.wan37.gameserver.game.quest.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wan37.gameserver.game.quest.model.*;
import com.wan37.gameserver.game.player.model.Player;
import com.wan37.gameserver.manager.task.WorkThreadPool;
import com.wan37.gameserver.util.FileUtil;

import com.wan37.mysql.pojo.entity.TQuestProgress;
import com.wan37.mysql.pojo.entity.TQuestProgressExample;
import com.wan37.mysql.pojo.entity.TQuestProgressKey;
import com.wan37.mysql.pojo.mapper.TQuestProgressMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author gonefuture  gonefuture@qq.com
 * time 2018/12/25 18:29
 * @version 1.00
 * Description: 任务成就管理
 */


@Component
@Slf4j
public class QuestManager {

    @Resource
    private TQuestProgressMapper tQuestProgressMapper;

    // 任务成就
    private static Cache<Integer, Quest> questCache = CacheBuilder.newBuilder()
            .removalListener(
                    notification -> log.info(notification.getKey() + " 任务成就被移除，原因是" + notification.getCause())
            ).build();




    public static Quest getQuest(Integer missionId) {
        return questCache.getIfPresent(missionId);
    }

    public static Map<Integer, Quest> allMission() {
        return questCache.asMap();
    }


    @PostConstruct
    public void init() {
        loadQuest();
    }

    private void loadQuest() {
        String path = FileUtil.getStringPath("gameData/quest.xlsx");
        MissionExcelUtil missionExcelUtil = new MissionExcelUtil(path);
        Map<Integer, Quest> missionMap = missionExcelUtil.getMap();

        missionMap.values().forEach(
                mission -> {
                    mission.getConditionsMap();
                    mission.getRewardThingsMap();
                    questCache.put(mission.getKey(),mission);}
        );
        log.info("任务成就资源加载完毕");
    }


    /**
     * 加载任务成就进度
     * @param player 玩家
     */
    public void loadQuestProgress(Player player) {
        TQuestProgressExample tMissionProgressExample = new TQuestProgressExample();
        tMissionProgressExample.or().andPlayerIdEqualTo(player.getId());
        List<TQuestProgress> tMissionProgressList =  tQuestProgressMapper.selectByExample(tMissionProgressExample);
        Map<Integer, QuestProgress> playerMissionProgressMap = new ConcurrentHashMap<>(15);

        tMissionProgressList.forEach(
                tMP -> {
                    QuestProgress mp = new QuestProgress();
                    mp.setPlayerId(tMP.getPlayerId());
                    mp.setQuestId(tMP.getQuestId());
                    mp.setBeginTime(tMP.getBeginTime());
                    mp.setEndTime(tMP.getEndTime());
                    mp.setQuestState(tMP.getQuestState());
                    // 加载任务实体
                    mp.setQuest(getQuest(mp.getQuestId()));
                    // 从数据库获取任务成就进度
                    Map<String, ProgressNumber>  questProgressMap = JSON.parseObject(tMP.getProgress(),
                            new TypeReference<Map<String,ProgressNumber>>(){});
                    log.debug("========== 从数据库加载的任务进程 {}",  mp);
                    Optional.ofNullable(questProgressMap).ifPresent( qP ->
                            mp.setProgressMap(questProgressMap)
                    );
                    playerMissionProgressMap.put(mp.getQuestId(),mp);
                    player.getQuestProgresses().put(mp.getQuestId(),mp);
                }
        );
        log.debug("玩家任务成就进度数据数据完毕 {}", playerMissionProgressMap);
    }



    /**
     *  创建或更新一个玩家任务成就进度记录
     * @param progress 新创建的进度
     */
    public  void saveOrUpdateMissionProgress(QuestProgress progress) {
        WorkThreadPool.threadPool.execute(() -> {
            TQuestProgressKey key = new TQuestProgressKey();
            key.setQuestId(progress.getQuestId());
            key.setPlayerId(progress.getPlayerId());
            TQuestProgress tQuestProgress = tQuestProgressMapper.selectByPrimaryKey(key);
            if (Objects.isNull(tQuestProgress)) {
                tQuestProgressMapper.insertSelective(progress);
            } else {
                tQuestProgressMapper.updateByPrimaryKeySelective(progress);
            }
        });
    }


    /**
     *  更新玩家任务进程
     * @param progress 更新玩家进程
     */
    public void updateQuestProgress(QuestProgress progress) {
        WorkThreadPool.threadPool.execute(() -> tQuestProgressMapper.updateByPrimaryKeySelective(progress) );
    }

    /**
     *  移除数据库玩家任务进程
     * @param playerId 玩家id
     * @param questId 任务id
     */
    public void removeQuestProgress(Long playerId, Integer questId) {
        TQuestProgressKey key = new TQuestProgressKey();
        key.setQuestId(questId);
        key.setPlayerId(playerId);
        WorkThreadPool.threadPool.execute(() -> tQuestProgressMapper.deleteByPrimaryKey(key) );
    }


}
