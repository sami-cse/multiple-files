package np.com.mshrestha.dropzonetest.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import np.com.mshrestha.dropzonetest.model.UploadedFile;
import np.com.mshrestha.dropzonetest.service.FileUploadService;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@Controller
public class FileUploadController {

	@Autowired
	private FileUploadService uploadService;

	@RequestMapping("/")
	public String home() {

		return "fileUploader";
	}

	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	public @ResponseBody List<UploadedFile> upload(MultipartHttpServletRequest request, HttpServletResponse response)
			throws IOException {

		// Getting uploaded files from the request object
		Map<String, MultipartFile> fileMap = request.getFileMap();

		// Maintain a list to send back the files info. to the client side
		List<UploadedFile> uploadedFiles = new ArrayList<UploadedFile>();

		// Iterate through the map
		for (MultipartFile multipartFile : fileMap.values()) {

			// Save the file to local disk
			saveFileToLocalDisk(multipartFile);

			UploadedFile fileInfo = getUploadedFileInfo(multipartFile);

			// Save the file info to database
			fileInfo = saveFileToDatabase(fileInfo);

			// adding the file info to the list
			uploadedFiles.add(fileInfo);
		}

		return uploadedFiles;
	}

	@RequestMapping(value = "/update", method = RequestMethod.POST)
	public @ResponseBody List<UploadedFile> update(MultipartHttpServletRequest request, HttpServletResponse response)
			throws IOException {

		Map<String, MultipartFile> fileMap = request.getFileMap();

		List<UploadedFile> uploadedFiles = new ArrayList<UploadedFile>();

		for (MultipartFile multipartFile : fileMap.values()) {

			saveFileToLocalDisk(multipartFile);
			UploadedFile fileInfo = getUploadedFileInfo(multipartFile);
			fileInfo = saveFileToDatabase(fileInfo);
			uploadedFiles.add(fileInfo);
		}

		return uploadedFiles;
	}

	@RequestMapping(value = { "/list" })
	public String listBooks(Map<String, Object> map) {
		map.put("fileList", uploadService.listFiles());
		return "/listFiles";
	}
	
	private Path path;
	@RequestMapping("/delete/{id}")
	public String delete(@PathVariable Long id,String name, Model model, HttpServletRequest request) {

		String rootDirectory = request.getSession().getServletContext().getRealPath("/");
		path = Paths.get(rootDirectory + "\\WEB-INF\\" + id + ".jpg");
		System.out.println(path);

		if (Files.exists(path)) {
			try {
				Files.delete(path);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		uploadService.delete(id);
		return "redirect:/list";
	}

	@RequestMapping(value = "/getBy/{fileId}", method = RequestMethod.GET)
	public String getById(Model model, @PathVariable Long fileId) {
		UploadedFile uploadedFile = uploadService.getFile(fileId);
		model.addAttribute("uploadedFile", uploadedFile);
		return "/edit";
	}

	@RequestMapping(value = "/get/{fileId}", method = RequestMethod.GET)
	public void getFile(HttpServletResponse response, @PathVariable Long fileId) {

		UploadedFile dataFile = uploadService.getFile(fileId);

		File file = new File(dataFile.getLocation(), dataFile.getName());

		try {
			response.setContentType(dataFile.getType());
			response.setHeader("Content-disposition", "attachment; filename=\"" + dataFile.getName() + "\"");

			FileCopyUtils.copy(FileUtils.readFileToByteArray(file), response.getOutputStream());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void saveFileToLocalDisk(MultipartFile multipartFile) throws IOException, FileNotFoundException {

		String outputFileName = getOutputFilename(multipartFile);

		FileCopyUtils.copy(multipartFile.getBytes(), new FileOutputStream(outputFileName));
	}

	private UploadedFile saveFileToDatabase(UploadedFile uploadedFile) {

		return uploadService.saveFile(uploadedFile);

	}

	private String getOutputFilename(MultipartFile multipartFile) {
		return getDestinationLocation() + multipartFile.getOriginalFilename();
	}

	private UploadedFile getUploadedFileInfo(MultipartFile multipartFile) throws IOException {

		UploadedFile fileInfo = new UploadedFile();
		fileInfo.setName(multipartFile.getOriginalFilename());
		fileInfo.setSize(multipartFile.getSize());
		fileInfo.setType(multipartFile.getContentType());
		fileInfo.setLocation(getDestinationLocation());

		return fileInfo;
	}

	private String getDestinationLocation() {
		return "/opt/multipleFileUploads/";
	}
}
