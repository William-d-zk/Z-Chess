/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.isahl.chess.knight.scheduler.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isahl.chess.knight.scheduler.domain.SubTaskStatus;
import com.isahl.chess.knight.scheduler.domain.Task;
import com.isahl.chess.knight.scheduler.domain.TaskResult;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DefaultResultAggregator implements ResultAggregator {
  private final ObjectMapper _Mapper;

  public DefaultResultAggregator(ObjectMapper mapper) {
    _Mapper = mapper;
  }

  @Override
  public String aggregate(List<TaskResult.SubTaskResultEntry> results) {
    try {
      return _Mapper.writeValueAsString(results);
    } catch (JsonProcessingException e) {
      return "[\""
          + results.stream().map(r -> r.getResult()).collect(Collectors.joining("\",\""))
          + "\"]";
    }
  }

  @Override
  public boolean canComplete(Task task) {
    return task.getSubTasks().stream()
        .allMatch(
            st ->
                st.getStatus() == SubTaskStatus.COMPLETE || st.getStatus() == SubTaskStatus.FAILED);
  }
}
