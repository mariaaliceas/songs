package br.com.songs.services.vacancies;

import br.com.songs.converter.ong.OngConverter;
import br.com.songs.domain.audit.LogSystem;
import br.com.songs.domain.entity.ActingArea;
import br.com.songs.domain.entity.Ong;
import br.com.songs.domain.entity.Vacancies;
import br.com.songs.exception.OperationException;
import br.com.songs.repository.ActingAreaRepository;
import br.com.songs.repository.VacanciesRepository;
import br.com.songs.services.audit.LogSystemService;
import br.com.songs.services.ong.OngService;
import br.com.songs.services.user.login.UserLoggedService;
import br.com.songs.web.dto.acting.ActingAreaDTO;
import br.com.songs.web.dto.ong.OngRequestGetDTO;
import br.com.songs.web.dto.vacancies.VacanciesRequestGetDTO;
import br.com.songs.web.dto.vacancies.VacanciesRequestPostDTO;
import br.com.songs.web.dto.vacancies.VacanciesRequestPutDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static br.com.songs.converter.ong.OngConverter.convertActingAreaEntityToActingAreaDTO;
import static br.com.songs.converter.vacancies.VacanciesConverter.*;

@Service
@AllArgsConstructor
@Data
public class VacanciesServiceImpl implements VacanciesService {
    private VacanciesRepository repository;
    private ActingAreaRepository actingAreaRepository;
    private OngService ongService;
    @Autowired
    private LogSystemService logSystemService;
    @Autowired
    private UserLoggedService userLoggedService;
    @Override
    public VacanciesRequestGetDTO findById(long id) {
        Optional<Vacancies> vacanciesOptional = repository.findById(id);

        if(vacanciesOptional.isEmpty()){
            throw new OperationException("not found");
        }

        return convertVacancies(vacanciesOptional.get());
    }

    @Override
    public List<VacanciesRequestGetDTO> findAll(Pageable pageable) {
        List<Vacancies> vacanciesList = repository.findAll(pageable).getContent();

        if(vacanciesList == null || vacanciesList.isEmpty()){
            return new ArrayList<>();
        }

        return convertVacancies(vacanciesList);
    }

    @Override
    public List<VacanciesRequestGetDTO> findByIdOng(long id, Pageable pageable) {
        List<Vacancies> vacanciesList = repository.findAllByIdOng(id, pageable);

        if(vacanciesList == null || vacanciesList.isEmpty()){
            return new ArrayList<>();
        }

        return convertVacancies(vacanciesList);
    }

    @Override
    public VacanciesRequestGetDTO saveOng(VacanciesRequestPostDTO vacanciesRequestPostDTO) {
        Vacancies vacancies = convertVacanciesRequestPostDTOToVacanciesEntity(vacanciesRequestPostDTO);
        Ong ong = ongService.findOngEntityById(vacanciesRequestPostDTO.getOng());
        vacancies.setOng(ong);
        logSystemService.createLog(LogSystem.CREATE_VACANCIES, ong.getId(), userLoggedService.getUserLogged().get().getId(), "create vacancies");
        return convertVacancies(repository.save(vacancies));
    }

    @Override
    public void updateOng(VacanciesRequestPutDTO vacanciesRequestPutDTO) {
        Vacancies vacancies = convertVacanciesRequestPutDTOToVacanciesEntity(vacanciesRequestPutDTO);

        if(vacancies.getId()==0){
            throw new OperationException("id vacancies not found");
        }

        Ong ong = ongService.findOngEntityById(vacanciesRequestPutDTO.getOng());
        vacancies.setOng(ong);
        convertVacancies(repository.save(vacancies));
        logSystemService.createLog(LogSystem.UPDATE_VACANCIES, ong.getId(), userLoggedService.getUserLogged().get().getId(), "update vacancies");
    }

    @Override
    public void deleteOngById(long id) {
        VacanciesRequestGetDTO vacanciesRequestGetDTO = findById(id);
        repository.deleteById(id);
        logSystemService.createLog(LogSystem.DELETE_VACANCIES,vacanciesRequestGetDTO.getOng().getId(), userLoggedService.getUserLogged().get().getId(), "delete vacancies");

    }

    private List<VacanciesRequestGetDTO> convertVacancies(List<Vacancies> vacanciesList){
        return vacanciesList.stream().map(this::convertVacancies).collect(Collectors.toList());
    }
    private VacanciesRequestGetDTO convertVacancies(Vacancies vacancies){
        VacanciesRequestGetDTO vacanciesRequestGetDTO = convertOVacanciesToOngRequestGetDTO(vacancies);
        OngRequestGetDTO ongRequestGetDTO = OngConverter.convertOngEntityToOngRequestGetDTO(vacancies.getOng());
        Optional<ActingArea> ongOptional = actingAreaRepository.findById(vacancies.getOng().getActingArea().getId());
        ActingAreaDTO actingAreaDTO = convertActingAreaEntityToActingAreaDTO(ongOptional.get());
        ongRequestGetDTO.setActingArea(actingAreaDTO);
        vacanciesRequestGetDTO.setOng(ongRequestGetDTO);
        return vacanciesRequestGetDTO;
    }
}
