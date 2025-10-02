/*
 *
 * Copyright 2014 Jules White
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
 *
 */
package org.magnum.dataup;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.magnum.dataup.VideoSvcApi.*;
import static org.magnum.dataup.VideoSvcApi.DATA_PARAMETER;
import static org.magnum.dataup.VideoSvcApi.ID_PARAMETER;
import static org.magnum.dataup.VideoSvcApi.VIDEO_DATA_PATH;

@Controller
public class VideosController {

    /**
     * You will need to create one or more Spring controllers to fulfill the
     * requirements of the assignment. If you use this file, please rename it
     * to something other than "AnEmptyController"
     * <p>
     * <p>
     * ________  ________  ________  ________          ___       ___  ___  ________  ___  __
     * |\   ____\|\   __  \|\   __  \|\   ___ \        |\  \     |\  \|\  \|\   ____\|\  \|\  \
     * \ \  \___|\ \  \|\  \ \  \|\  \ \  \_|\ \       \ \  \    \ \  \\\  \ \  \___|\ \  \/  /|_
     * \ \  \  __\ \  \\\  \ \  \\\  \ \  \ \\ \       \ \  \    \ \  \\\  \ \  \    \ \   ___  \
     * \ \  \|\  \ \  \\\  \ \  \\\  \ \  \_\\ \       \ \  \____\ \  \\\  \ \  \____\ \  \\ \  \
     * \ \_______\ \_______\ \_______\ \_______\       \ \_______\ \_______\ \_______\ \__\\ \__\
     * \|_______|\|_______|\|_______|\|_______|        \|_______|\|_______|\|_______|\|__| \|__|
     *
     *
     */

    private final VideoFileManager videoDataMgr = VideoFileManager.get();
    private static final AtomicLong currentId = new AtomicLong(0L);
    private final Map<Long, Video> videos = new HashMap<>();

    public VideosController() throws IOException {
    }

    @RequestMapping(value = VIDEO_SVC_PATH, method = RequestMethod.POST)
    public @ResponseBody Video addVideo(@RequestBody Video v) {
        checkAndSetId(v);
        v.setDataUrl(getDataUrl(v.getId()));
        videos.put(v.getId(), v);
        return new ResponseEntity<>(v, HttpStatus.OK).getBody();
    }

    @RequestMapping(value = VIDEO_SVC_PATH, method = RequestMethod.GET)
    public @ResponseBody Collection<Video> getVideoList() {
        return new ResponseEntity<>(videos.values(), HttpStatus.OK).getBody();
    }

    @RequestMapping(value = VIDEO_DATA_PATH, method = RequestMethod.POST)
    public @ResponseBody VideoStatus setVideoData(
            @PathVariable(ID_PARAMETER) Long id,
            @RequestPart(DATA_PARAMETER) MultipartFile data
    ) throws Exception {
        Video video = videos.get(id);
        try {
            videoDataMgr.hasVideoData(video);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        videoDataMgr.saveVideoData(video, data.getInputStream());
        return ResponseEntity.ok(new VideoStatus(VideoStatus.VideoState.READY)).getBody();
    }

    @RequestMapping(value = VIDEO_DATA_PATH, method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<?> getData(
            @PathVariable(ID_PARAMETER) long id,
            HttpServletResponse response
    ) throws IOException {
        ServletOutputStream outputStream = response.getOutputStream();
        try {
            videoDataMgr.copyVideoData(videos.get(id), outputStream);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok().build();
    }



    private void checkAndSetId(Video entity) {
        if (entity.getId() == 0) {
            entity.setId(currentId.incrementAndGet());
        }
    }

    private String getDataUrl(long videoId) {
        return getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
    }

    private String getUrlBaseForLocalServer() {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        return "http://" + request.getServerName()
                + ((request.getServerPort() != 80) ? ":" + request.getServerPort() : "");

    }

}
