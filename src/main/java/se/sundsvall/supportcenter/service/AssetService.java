package se.sundsvall.supportcenter.service;

import static java.util.Collections.emptyList;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.supportcenter.service.mapper.ConfigurationMapper.toPobPayload;
import static se.sundsvall.supportcenter.service.mapper.CreateAssetMapper.toItemId;
import static se.sundsvall.supportcenter.service.mapper.CreateAssetMapper.toItemIdList;
import static se.sundsvall.supportcenter.service.mapper.CreateAssetMapper.toPobPayloadForCreatingConfigurationItem;
import static se.sundsvall.supportcenter.service.mapper.CreateAssetMapper.toPobPayloadForCreatingItem;
import static se.sundsvall.supportcenter.service.mapper.GetAssetMapper.toAssetList;
import static se.sundsvall.supportcenter.service.mapper.GetAssetMapper.toItemId;
import static se.sundsvall.supportcenter.service.mapper.GetAssetMapper.toMapOfAttributes;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.KEY_CONFIGURATION_ITEM;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.TYPE_CONFIGURATION_ITEM;
import static se.sundsvall.supportcenter.service.mapper.constant.ConfigurationMapperConstants.TYPE_ITEM;

import generated.client.pob.PobPayload;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import se.sundsvall.supportcenter.api.model.Asset;
import se.sundsvall.supportcenter.api.model.CreateAssetRequest;
import se.sundsvall.supportcenter.api.model.UpdateAssetRequest;
import se.sundsvall.supportcenter.integration.pob.POBClient;

@Service
public class AssetService {

	private static final String CI_ID_ERROR_TEMPLATE = "No ID was found in POB-configurationitems";

	private final POBClient pobClient;
	private final ConfigurationService configurationService;

	public AssetService(POBClient pobClient, ConfigurationService configurationService) {
		this.configurationService = configurationService;
		this.pobClient = pobClient;
	}

	public String createAsset(final String pobKey, final CreateAssetRequest createAssetRequest) {

		var id = "";
		final var itemIds = Optional.ofNullable(getItemsByModelName(pobKey, createAssetRequest.getModelName())).orElse(emptyList());

		if (itemIds.isEmpty()) {
			id = createItem(pobKey, createAssetRequest);
		} else {
			id = itemIds.stream().findFirst().orElseThrow(() -> Problem.valueOf(Status.BAD_GATEWAY, CI_ID_ERROR_TEMPLATE));
		}
		return createConfigurationItem(pobKey, id, createAssetRequest);
	}

	public void updateConfigurationItem(final String pobKey, final String serialNumber, final UpdateAssetRequest request) {
		pobClient.updateConfigurationItem(pobKey, toPobPayload(configurationService.getSerialNumberId(pobKey, serialNumber), request));
	}

	public List<Asset> getConfigurationItemsBySerialNumber(final String pobKey, final String serialNumber) {

		final var configurationItems = pobClient.getConfigurationItemsBySerialNumber(pobKey, serialNumber);
		final var itemId = toItemId(configurationItems);
		List<PobPayload> item = null;
		if (hasText(itemId)) {
			item = pobClient.getItemsById(pobKey, itemId);
		}

		final var itemAttributes = toMapOfAttributes(item);

		return toAssetList(configurationItems, itemAttributes);
	}

	private String createItem(final String pobKey, final CreateAssetRequest request) {

		return toItemId(pobClient.createItem(pobKey, toPobPayloadForCreatingItem(request)), TYPE_ITEM, KEY_CONFIGURATION_ITEM);
	}

	private String createConfigurationItem(final String pobKey, final String id, final CreateAssetRequest request) {

		return toItemId(pobClient.createConfigurationItem(pobKey, toPobPayloadForCreatingConfigurationItem(id, request)), TYPE_CONFIGURATION_ITEM, KEY_CONFIGURATION_ITEM);
	}

	private List<String> getItemsByModelName(final String pobKey, final String modelName) {
		return toItemIdList(pobClient.getItemsByModelName(pobKey, modelName));
	}
}
