package com.zhaoss.weixinrecorded.util;

import com.lansosdk.videoeditor.LanSongFileUtil;
import com.lansosdk.videoeditor.VideoEditor;

import java.util.ArrayList;
import java.util.List;

/**
 * MyVideoEditor
 *
 * @blame Android Team
 */
public class MyVideoEditor extends VideoEditor {

    public boolean h264ToTs(String src, String des){

        ArrayList<String> list = new ArrayList<>();
        list.add("-i");
        list.add(src);
        list.add("-vcodec");
        list.add("copy");
        list.add("-vbsf");
        list.add("h264_mp4toannexb");
        list.add(des);

        int i = executeVideoEditor(list.toArray(new String[]{}));
        return i == 0;
    }

    public boolean h264ToMp4(String src, String des){

        ArrayList<String> list = new ArrayList<>();
        list.add("-i");
        list.add(src);
        list.add("-vcodec");
        list.add("copy");
        list.add("-f");
        list.add("mp4");
        list.add(des);

        int i = executeVideoEditor(list.toArray(new String[]{}));
        return i == 0;
    }

    public int concatAudio(String[] tsArray, String dstFile) {
        // ffmpeg -i "concat:a.mp3|a1.mp3" -acodec copy a2.mp3
        if (LanSongFileUtil.filesExist(tsArray)) {
            String concat = "concat:";
            for (int i = 0; i < tsArray.length - 1; i++) {
                concat += tsArray[i];
                concat += "|";
            }
            concat += tsArray[tsArray.length - 1];

            List<String> cmdList = new ArrayList<String>();

            cmdList.add("-i");
            cmdList.add(concat);

            cmdList.add("-c");
            cmdList.add("copy");

            cmdList.add("-y");

            cmdList.add(dstFile);
            String[] command = new String[cmdList.size()];
            for (int i = 0; i < cmdList.size(); i++) {
                command[i] = (String) cmdList.get(i);
            }
            return executeVideoEditor(command);
        } else {
            return -1;
        }
    }
}
