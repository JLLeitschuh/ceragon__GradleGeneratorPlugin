package io.github.ceragon.protobuf.protoc;


import io.github.ceragon.PluginContext;
import io.github.ceragon.protobuf.bean.ProtoFileDescPojo;
import io.github.ceragon.protobuf.bean.ProtoMessageDescPojo;
import io.github.ceragon.protobuf.extension.EveryMsgBuildConfig;
import io.github.ceragon.protobuf.extension.EveryProtoBuildConfig;
import io.github.ceragon.protobuf.extension.TotalMsgBuildConfig;
import io.github.ceragon.util.CodeGenTool;
import io.github.ceragon.util.PathFormat;
import io.github.ceragon.util.VelocityUtil;
import lombok.Value;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Value
public class MsgCodeBuild {
    List<ProtoFileDescPojo> protoFileDescPojoList;

    //region -------- 使用全部消息的集合生成代码 ------------
    public boolean buildTotalMsgCode(String resourceRoot, Set<TotalMsgBuildConfig> configList) {
        Map<String, Object> content = new HashMap<>();
        List<ProtoMessageDescPojo> messageDescPojoList = protoFileDescPojoList.stream().flatMap(pojo -> pojo.getMessageList().stream())
                .collect(Collectors.toList());
        content.put("totalMsgList", messageDescPojoList);
        content.put("totalMsgGroupList", protoFileDescPojoList);
        return configList.stream().allMatch(config -> processTotalMsgCode(resourceRoot, config, content));
    }

    private boolean processTotalMsgCode(String resourceRoot, TotalMsgBuildConfig config, Map<String, Object> content) {
        PathFormat pathFormat = PluginContext.pathFormat();
        String sourceName = config.getVmFile();
        String destPath = pathFormat.format(config.getTargetFile());
        try {
            CodeGenTool.createCodeByPath(resourceRoot, sourceName, destPath, config.isOverwrite(), content);
            PluginContext.log().quiet("totalMsgCode generate success!name={}", config.getName());
            return true;
        } catch (IOException e) {
            PluginContext.log().error(e.getMessage(), e);
            return false;
        }
    }
    //endregion

    //region -------- 使用every消息的集合生成代码 ------------
    public boolean buildEveryMsgCode(String resourceRoot, Set<EveryMsgBuildConfig> everyMsg) {
        return everyMsg.stream().allMatch(config -> processEveryMsgCodeConfig(resourceRoot, config));
    }

    private boolean processEveryMsgCodeConfig(String resourceRoot, EveryMsgBuildConfig config) {
        return this.protoFileDescPojoList.stream()
                .filter(pojo -> config.getProtoNameMatch().stream().anyMatch(protoNameMatch -> pojo.getName().matches(protoNameMatch)))
                .allMatch(pojo -> processEveryMsgCodePojoAndConfig(resourceRoot, pojo, config));
    }

    private boolean processEveryMsgCodePojoAndConfig(String resourceRoot, ProtoFileDescPojo pojo, EveryMsgBuildConfig config) {
        return pojo.getMessageList().stream()
                .filter(protoMessagePojo -> config.getMsgNameMatch().stream().anyMatch(msgNameMatch -> protoMessagePojo.getName().matches(msgNameMatch)))
                .allMatch(protoMessagePojo -> processEveryMsgCode(resourceRoot, protoMessagePojo, config));
    }

    private boolean processEveryMsgCode(String resourceRoot, ProtoMessageDescPojo protoMessagePojo, EveryMsgBuildConfig config) {
        try {
            Map<String, Object> content = new HashMap<>();
            content.put("msg", protoMessagePojo);
            content.put("util", VelocityUtil.getInstance());
            String destPath = PluginContext.pathFormat().format(config.getTargetFile(), "MsgName", protoMessagePojo.getName());
            CodeGenTool.createCodeByPath(resourceRoot, config.getVmFile(), destPath, config.isOverwrite(), content);
            PluginContext.log().quiet("everyMsgCode generate success!name={}", config.getName());
            return true;
        } catch (IOException e) {
            PluginContext.log().error(e.getMessage(), e);
            return false;
        }
    }

    //endregion

    public boolean buildEveryProtoCode(String resourceRoot, Set<EveryProtoBuildConfig> everyProto) {
        return everyProto.stream().allMatch(config -> processEveryProtoCodeConfig(resourceRoot, config));
    }

    private boolean processEveryProtoCodeConfig(String resourceRoot, EveryProtoBuildConfig config) {
        return this.protoFileDescPojoList.stream()
                .filter(pojo -> config.getProtoNameMatch().stream().anyMatch(protoNameMatch -> pojo.getName().matches(protoNameMatch)))
                .allMatch(pojo -> processEveryProtoCode(resourceRoot, pojo, config));
    }

    private boolean processEveryProtoCode(String resourceRoot, ProtoFileDescPojo protoFileDescPojo, EveryProtoBuildConfig config) {
        try {
            Map<String, Object> content = new HashMap<>();
            content.put("proto", protoFileDescPojo);
            content.put("util", VelocityUtil.getInstance());
            String destPath = PluginContext.pathFormat().format(config.getTargetFile());
            CodeGenTool.createCodeByPath(resourceRoot, config.getVmFile(), destPath, config.isOverwrite(), content);

            PluginContext.log().quiet("everyProtoCode generate success!name={}", config.getName());
            return true;
        } catch (IOException e) {
            PluginContext.log().error(e.getMessage(), e);
            return false;
        }
    }


}