package hu.webuni.hr.minta.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.annotation.JsonView;

import hu.webuni.hr.minta.dto.CompanyDto;
import hu.webuni.hr.minta.dto.EmployeeDto;
import hu.webuni.hr.minta.dto.View.BaseData;


@RestController
@RequestMapping("/api/companies")
public class CompanyController {

	private Map<Long, CompanyDto> companies = new HashMap<>();
	
	//1. megoldás
//	@GetMapping
//	public List<CompanyDto> getAll(@RequestParam(required = false) Boolean full){
//
//		if(isFull(full)) {
//			return new ArrayList<>(companies.values());
//		} else {
//			return companies.values().stream()
//					.map(this::createCompanyWithoutEmployees)
//					.collect(Collectors.toList());
//		}
//	}
	
	private CompanyDto createCompanyWithoutEmployees(CompanyDto c) {
		return new CompanyDto(c.getId(), c.getRegistrationNumber(), c.getName(), c.getAddress(), null);
	}
	
	private boolean isFull(Boolean full) {
		return full != null && full;
	}
	
	//2. megoldás
	@GetMapping(params = "full=true")
	public List<CompanyDto> getAllWithEmployees(){
		return new ArrayList<>(companies.values());
	}
	
	@GetMapping
	@JsonView(BaseData.class)
	public List<CompanyDto> getAllWithoutEmployees(@RequestParam(required = false) Boolean full){
		return new ArrayList<>(companies.values());
	}
	
	@GetMapping("/{id}")
	public CompanyDto getById(@PathVariable long id, @RequestParam(required = false) Boolean full) {
		CompanyDto companyDto = companies.get(id);
		if(isFull(full))
			return companyDto;
		else
			return createCompanyWithoutEmployees(companyDto);
	}
	
	@PostMapping
	public CompanyDto createCompany(@RequestBody CompanyDto companyDto) {
		companies.put(companyDto.getId(), companyDto);
		return companyDto;
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<CompanyDto> modifyCompany(@PathVariable long id, @RequestBody CompanyDto companyDto) {
		if(!companies.containsKey(id)) {
			return ResponseEntity.notFound().build();
		}
		
		companyDto.setId(id);
		companies.put(id, companyDto);
		return ResponseEntity.ok(companyDto);
	}
	
	@DeleteMapping("/{id}")
	public void deleteCompany(@PathVariable long id) {
		companies.remove(id);
	}
	
	
	@PostMapping("/{id}/employees")
	public CompanyDto addNewEmployee(@PathVariable long id, @RequestBody EmployeeDto employeeDto){
		CompanyDto company = findByIdOrThrow(id);
		
		company.getEmployees().add(employeeDto);
		return company;
	}

	private CompanyDto findByIdOrThrow(long id) {
		CompanyDto company = companies.get(id);
		if(company == null)
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		return company;
	}

	@DeleteMapping("/{id}/employees/{employeeId}")
	public CompanyDto deleteEmployeeFromCompany(@PathVariable long id, @PathVariable long employeeId) {
		CompanyDto company = findByIdOrThrow(id);
		company.getEmployees().removeIf(emp -> emp.getId() == employeeId);
		return company;
	}
	
	@PutMapping("/{id}/employees")
	public CompanyDto replaceAllEmployees(@PathVariable long id, @RequestBody List<EmployeeDto> employees) {
		CompanyDto company = findByIdOrThrow(id);
		company.setEmployees(employees);
		return company;
	}
	
}
