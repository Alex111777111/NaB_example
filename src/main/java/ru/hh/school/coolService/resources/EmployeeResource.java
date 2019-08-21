package ru.hh.school.coolService.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.hh.school.coolService.dto.EmployeeCreateDto;
import ru.hh.school.coolService.dto.EmployeeDto;
import ru.hh.school.coolService.dto.ResumeCreateDto;
import ru.hh.school.coolService.dto.ResumeDto;
import ru.hh.school.coolService.services.EmployeeService;
import ru.hh.school.coolService.spring.RestTimeout;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/")
@RestTimeout(key = "resttimeout.sup.param.name", value = "65000")
@Singleton
public class EmployeeResource {

  public static final int MILLIS = 60000;
  Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private RestTestBean bean;

/*
  @Autowired
*/
  private EmployeeService employeeService;

  public EmployeeResource(EmployeeService employeeService) {
    this.employeeService = employeeService;
  }

  @GET
  @Produces("application/json")
  @Path("/employee/{id}")
  @ResponseBody
  public Response getEmployee(@PathParam("id") Integer id) {
    return Response.status(Response.Status.OK)
        .entity(new EmployeeDto(employeeService.getEmployeeById(id)))
        .build();
  }

  @POST
  @Produces("application/json")
  @Path("/employee/create")
  @ResponseBody
  public Response createEmployee(@RequestBody EmployeeCreateDto employeeCreateDto){
    return Response.status(Response.Status.OK)
        .entity(new EmployeeDto(employeeService.createEmployee(employeeCreateDto)))
        .build();
  }

  @GET
  @Produces("application/json")
  @Path("/resume/{id}")
  @ResponseBody
  public Response getResume(@PathParam("id") Integer id) {
    return Response.status(Response.Status.OK)
        .entity(new ResumeDto(employeeService.getResumeById(id)))
        .build();
  }

  @POST
  @Produces("application/json")
  @Path("/resume/create")
  @ResponseBody
  public Response createResume(@RequestBody ResumeCreateDto resumeCreateDto){
    return Response.status(Response.Status.OK)
        .entity(new ResumeDto(employeeService.createResume(resumeCreateDto)))
        .build();
  }

  @GET
  @Produces("application/json")
  @Path("/resume2/{id}")
  public Response getEchoRequest(@PathParam("id") Integer id) {
    return Response.status(Response.Status.OK)
            .entity("\nSome message from server: " + id)
            .build();
  }

  @GET
  @Produces("application/json")
  @Path("/resume3/{id}")
  @RestTimeout(key = "getEchoRequest3.timeout.param", value = "20000")
  public Response getEchoRequest3(@PathParam("id") Integer id) throws InterruptedException {
    logger.info("===> getEchoRequest3(): " + id);

/*
    try {
*/
      logger.info("getEchoRequest3(): thread sleeping " + MILLIS);
      Thread.sleep(MILLIS);
/*
    } catch (InterruptedException e) {
      logger.error("getEchoRequest3(): " + e.getMessage(), e);
    }
*/

    final Response res = Response.status(Response.Status.OK)
            .entity("\nSome message3 from server: " + id)
            .build();
    logger.info("<=== getEchoRequest3(): " + id + ", " + res.toString());
    return res;
  }


  public String getTest() {
    return "22244455";
  }

}
