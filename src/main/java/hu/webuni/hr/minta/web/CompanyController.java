package hu.webuni.hr.minta.web;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.data.web.SortDefault.SortDefaults;
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

import hu.webuni.hr.minta.dto.CompanyDto;
import hu.webuni.hr.minta.dto.EmployeeDto;
import hu.webuni.hr.minta.mapper.CompanyMapper;
import hu.webuni.hr.minta.mapper.EmployeeMapper;
import hu.webuni.hr.minta.model.AverageSalaryByPosition;
import hu.webuni.hr.minta.model.Company;
import hu.webuni.hr.minta.repository.CompanyRepository;
import hu.webuni.hr.minta.service.CompanyService;


@RestController
@RequestMapping("/api/companies")
public class CompanyController {
	

	private CompanyMapper companyMapper;
	private CompanyService companyService;
	private EmployeeMapper employeeMapper;
	private CompanyRepository companyRepository;
	

	public CompanyController(CompanyMapper companyMapper, CompanyService companyService, EmployeeMapper employeeMapper,
			CompanyRepository companyRepository) {
		super();
		this.companyMapper = companyMapper;
		this.companyService = companyService;
		this.employeeMapper = employeeMapper;
		this.companyRepository = companyRepository;
	}

	//1. megoldás
	@GetMapping
	public List<CompanyDto> getAll(@RequestParam(required = false) Boolean full){
		boolean isFull = isFull(full);

		List<Company> companies = isFull ? companyRepository.findAllWithEmployees() : companyService.findAll();
		
		if(isFull) {
			return companyMapper.companiesToDtos(companies);
		} else {
			return companyMapper.companiesToSummaryDtos(companies);
		}
	}
	
//	private CompanyDto createCompanyWithoutEmployees(CompanyDto c) {
//		return new CompanyDto(c.getId(), c.getRegistrationNumber(), c.getName(), c.getAddress(), null);
//	}
	
	private boolean isFull(Boolean full) {
		return full != null && full;
	}
	
	//2. megoldás
//	@GetMapping(params = "full=true")
//	public List<CompanyDto> getAllWithEmployees(){
//		return new ArrayList<>(companies.values());
//	}
	
//	@GetMapping
//	@JsonView(BaseData.class)
//	public List<CompanyDto> getAllWithoutEmployees(@RequestParam(required = false) Boolean full){
//		return new ArrayList<>(companies.values());
//	}
	
	@GetMapping("/{id}")
	public CompanyDto getById(@PathVariable long id, @RequestParam(required = false) Boolean full) {
		boolean isFull = isFull(full);
		Company company = isFull ? companyRepository.findByIdWithEmployees(id).get() : findByIdOrThrow(id);
		if(isFull)
			return companyMapper.companyToDto(company);
		else
			return companyMapper.companyToSummaryDto(company);
	}
	
	@PostMapping
	public CompanyDto createCompany(@RequestBody CompanyDto companyDto) {
      return companyMapper.companyToDto(companyService.save(companyMapper.dtoToCompany(companyDto)));
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<CompanyDto> modifyCompany(@PathVariable long id, @RequestBody CompanyDto companyDto) {
      companyDto.setId(id);
      Company updatedCompany = companyService.update(companyMapper.dtoToCompany(companyDto));
      if (updatedCompany == null) {
          return ResponseEntity.notFound().build();
      }

      return ResponseEntity.ok(companyMapper.companyToDto(updatedCompany));
	}
	
	@DeleteMapping("/{id}")
	public void deleteCompany(@PathVariable long id) {
		companyService.delete(id);
	}
	
	@PostMapping("/{companyId}/employees")
	public CompanyDto addNewEmployee(@PathVariable long companyId, @RequestBody EmployeeDto employeeDto) {
		try {
			return companyMapper
					.companyToDto(companyService.addEmployee(companyId, employeeMapper.dtoToEmployee(employeeDto)));
		} catch (NoSuchElementException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
	}

	private Company findByIdOrThrow(long id) {
		return companyService.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	@DeleteMapping("/{companyId}/employees/{empId}")
	public CompanyDto deleteEmployee(@PathVariable long companyId, @PathVariable long empId) {
		try {
			return companyMapper.companyToDto(companyService.deleteEmployee(companyId, empId));
		} catch (NoSuchElementException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
	}
	
	@PutMapping("/{companyId}/employees")
	public CompanyDto replaceEmployees(@PathVariable long companyId, @RequestBody List<EmployeeDto> newEmployees){
      try {
          return companyMapper.companyToDto(
                  companyService.replaceEmployees(companyId, employeeMapper.dtosToEmployees(newEmployees))
                  );
      } catch (NoSuchElementException e) {
          throw new ResponseStatusException(HttpStatus.NOT_FOUND);
      }
	}
	
	@GetMapping(params = "aboveSalary")
	public List<CompanyDto> getCompaniesAboveASalary(@RequestParam int aboveSalary,
			@RequestParam(required = false) Boolean full,
			@SortDefault("id") Pageable pageable) {
		Page<Company> page = companyRepository.findByEmployeeWithSalaryHigherThan(pageable, aboveSalary);
		System.out.println(page.getTotalElements());
		System.out.println(page.isLast());
		List<Company> allCompanies = page.getContent();
		return mapCompanies(allCompanies, full);
	}

	private List<CompanyDto> mapCompanies(List<Company> allCompanies, Boolean full) {
		if (full == null || !full) {
			return companyMapper.companiesToSummaryDtos(allCompanies);
		} else
			return companyMapper.companiesToDtos(allCompanies);
	}

	@GetMapping(params = "aboveEmployeeNumber")
	public List<CompanyDto> getCompaniesAboveEmployeeNumber(@RequestParam int aboveEmployeeNumber,
			@RequestParam(required = false) Boolean full) {
		List<Company> filteredCompanies = companyRepository.findByEmployeeCountHigherThan(aboveEmployeeNumber);
		return mapCompanies(filteredCompanies, full);
	}
	
	@GetMapping("/{id}/salaryStats")
	public List<AverageSalaryByPosition> getSalaryStatsById(@PathVariable long id, @RequestParam(required = false) Boolean full) {
		return companyRepository.findAverageSalariesByPosition(id);
	}
}
