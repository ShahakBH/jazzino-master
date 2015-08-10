package com.yazino.controller;

import com.yazino.model.log.IncrementalLog;
import com.yazino.model.ReadableGameStatusSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.repository.table.TableRepository;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;

@Controller
public class UnderTheHoodController {

    private final ReadableGameStatusSource gameStatusSource;
    private final TableRepository tableRepository;
    private final IncrementalLog gameHostLog;
    private final IncrementalLog documentLog;

    @Autowired
    public UnderTheHoodController(final ReadableGameStatusSource gameStatusSource,
                                  @Qualifier("gameHostLog") final IncrementalLog gameHostLog,
                                  @Qualifier("documentLog") final IncrementalLog documentLog,
                                  final TableRepository tableRepository) {
        this.gameStatusSource = gameStatusSource;
        this.gameHostLog = gameHostLog;
        this.documentLog = documentLog;
        this.tableRepository = tableRepository;
    }

    @RequestMapping("/logs/gameStatus")
    public ModelAndView gameStatus() {
        return new ModelAndView("logs/gameStatus", "status", gameStatusSource.getStatus());
    }

    @RequestMapping("/logs/gameHost")
    public ModelAndView commands() {
        return new ModelAndView("logs/gameHost");
    }

    @RequestMapping("/logs/gameHostChunk")
    public void commandsChunk(final HttpServletResponse response) throws IOException {
        writeJSON(response, gameHostLog.nextIncrement());
    }

    @RequestMapping("/logs/documents")
    public ModelAndView documents() {
        return new ModelAndView("logs/documents");
    }

    @RequestMapping("/logs/documentsChunk")
    public void documentsChunk(final HttpServletResponse response) throws IOException {
        writeJSON(response, documentLog.nextIncrement());
    }

    @RequestMapping("/logs/tableStatus")
    public void tableStatus(final HttpServletResponse response) throws IOException {
        final Table table = tableRepository.findById(BigDecimal.ONE);
        response.setContentType("text/plain");
        final PrintWriter writer = response.getWriter();
        writer.write(table.getTableStatus().toString());
        writer.flush();
    }


    @RequestMapping("/logs/fullTableStatus")
    public ModelAndView fullTableStatus() {
        final Table table = tableRepository.findById(BigDecimal.ONE);
        final ModelAndView modelAndView = new ModelAndView("logs/fullTableStatus");
        modelAndView.addObject("monitoringMessage", table.getMonitoringMessage());
        modelAndView.addObject("status", table.getTableStatus());
        return modelAndView;
    }

    private void writeJSON(final HttpServletResponse response, final String json) throws IOException {
        response.setContentType("application/json");
        final PrintWriter writer = response.getWriter();
        writer.write(json);
        writer.flush();
    }

}
