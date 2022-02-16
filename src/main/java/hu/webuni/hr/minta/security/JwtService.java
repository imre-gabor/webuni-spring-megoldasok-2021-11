package hu.webuni.hr.minta.security;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import hu.webuni.hr.minta.config.HrConfigProperties;
import hu.webuni.hr.minta.config.HrConfigProperties.JwtData;
import hu.webuni.hr.minta.model.Employee;
import hu.webuni.hr.minta.model.HrUser;

@Service
public class JwtService {

	private static final String MANAGED_EMPLOYEE_USERNAMES = "managedEmployeeUsernames";
	private static final String MANAGED_EMPLOYEE_IDS = "managedEmployeeIds";
	private static final String USERNAME = "username";
	private static final String MANAGER = "manager";
	private static final String ID = "id";
	private static final String FULLNAME = "fullname";
	private static final String AUTH = "auth";
	
	@Autowired
	private HrConfigProperties hrConfigProperties;
	
	private Algorithm alg;
	private String issuer;
	
	@PostConstruct
	public void init() {
		JwtData jwtData = hrConfigProperties.getJwtData();
		issuer = jwtData.getIssuer();

		//alg = Algorithm.HMAC256("mysecret");
		try {
			alg = (Algorithm) Algorithm.class.getMethod(jwtData.getAlg(), String.class)
					.invoke(Algorithm.class, jwtData.getSecret());
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			e.printStackTrace();
		}
	}
	
	public String creatJwtToken(UserDetails principal) {
		Employee employee = ((HrUser)principal).getEmployee();
		Employee manager = employee.getManager();
		List<Employee> managedEmployees = employee.getManagedEmployees();
		
		
		Builder jwtBuilder = JWT.create()
			.withSubject(principal.getUsername())
			.withArrayClaim(AUTH, principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toArray(String[]::new))
			.withClaim(FULLNAME, employee.getName())
			.withClaim(ID, employee.getEmployeeId());
		
		if(manager != null) {
			jwtBuilder
				.withClaim(MANAGER, Map.of(
						ID, manager.getEmployeeId(),
						USERNAME, manager.getUsername()
						));
		}
		
		
		if(managedEmployees != null && !managedEmployees.isEmpty()) {
			jwtBuilder
				.withArrayClaim(MANAGED_EMPLOYEE_IDS, 
					managedEmployees.stream().map(Employee::getEmployeeId).toArray(Long[]::new)
					)
				.withArrayClaim(MANAGED_EMPLOYEE_USERNAMES, 
						managedEmployees.stream().map(Employee::getUsername).toArray(String[]::new)
				);
		}
		
		return jwtBuilder
			.withExpiresAt(new Date(System.currentTimeMillis() + hrConfigProperties.getJwtData().getDuration().toMillis()))
			.withIssuer(issuer)
			.sign(alg);
		
	}

	public UserDetails parseJwt(String jwtToken) {
		
		DecodedJWT decodedJwt = JWT.require(alg)
			.withIssuer(issuer)
			.build()
			.verify(jwtToken);
		
		Employee employee = new Employee();
		
		employee.setEmployeeId(decodedJwt.getClaim(ID).asLong());
		employee.setUsername(decodedJwt.getSubject());
		employee.setName(decodedJwt.getClaim(FULLNAME).asString());
		
		Claim managerClaim = decodedJwt.getClaim(MANAGER);
		if(managerClaim != null) {
			Employee manager = new Employee();
			employee.setManager(manager);
			Map<String, Object> managerData = managerClaim.asMap();
			if(managerData != null) {
				manager.setEmployeeId(((Integer)managerData.get(ID)).longValue());
				manager.setUsername((String) managerData.get(USERNAME));
			}
		}
		
		Claim managedEmployeeUsernamesClaim = decodedJwt.getClaim(MANAGED_EMPLOYEE_USERNAMES);
		if(managedEmployeeUsernamesClaim  != null) {
			employee.setManagedEmployees(new ArrayList<>());
			List<String> managedEmployeeUsernames = managedEmployeeUsernamesClaim.asList(String.class);
			if(managedEmployeeUsernames != null && !managedEmployeeUsernames.isEmpty()) {
				List<Long> managedEmployeeIds = decodedJwt.getClaim(MANAGED_EMPLOYEE_IDS).asList(Long.class);
				for(int i=0; i < managedEmployeeUsernames.size(); i++) {
					Employee managedEmployee = new Employee();
					managedEmployee.setEmployeeId(managedEmployeeIds.get(i));
					managedEmployee.setUsername(managedEmployeeUsernames.get(i));
					employee.getManagedEmployees().add(managedEmployee);
				}
			}
		}
		
		return new HrUser(decodedJwt.getSubject(), "dummy", 
				decodedJwt.getClaim(AUTH).asList(String.class).stream()
				.map(SimpleGrantedAuthority::new).collect(Collectors.toList()), employee);
		
	}

}
