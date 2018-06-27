package org.ovirt.engine.core.bll.storage.disk.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.ovirt.engine.core.bll.ValidateTestUtils;
import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.validator.storage.DiskImagesValidator;
import org.ovirt.engine.core.bll.validator.storage.DiskValidator;
import org.ovirt.engine.core.bll.validator.storage.StorageDomainValidator;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.TransferDiskImageParameters;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.StorageType;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.DiskImageDao;
import org.ovirt.engine.core.dao.StorageDomainDao;

@RunWith(MockitoJUnitRunner.class)
public class TransferDiskImageCommandTest extends TransferImageCommandTest {

    @Mock
    DiskValidator diskValidator;

    @Mock
    DiskImagesValidator diskImagesValidator;

    @Mock
    StorageDomainValidator storageDomainValidator;

    @Mock
    StorageDomainDao storageDomainDao;

    @Mock
    DiskImageDao diskImageDao;

    @Override
    protected TransferDiskImageCommand spyCommand() {
        return new TransferDiskImageCommand(new TransferDiskImageParameters(), null);
    }

    @Before
    public void setUp() {
        initCommand();
    }

    protected void initializeSuppliedImage() {
        super.initSuppliedImage(transferImageCommand);

        DiskImage diskImage = new DiskImage();
        diskImage.setActive(true);
        diskImage.setImageId(transferImageCommand.getParameters().getImageId());
        diskImage.setStorageIds(new ArrayList<>(Collections.singletonList(Guid.newGuid())));
        diskImage.setStorageTypes(new ArrayList<>(Collections.singletonList(StorageType.NFS)));
        doReturn(diskImage).when(diskImageDao).get(any());

        doReturn(diskValidator).when(getCommand()).getDiskValidator(any());
        doReturn(diskImagesValidator).when(getCommand()).getDiskImagesValidator(any());
        doReturn(storageDomainValidator).when(getCommand()).getStorageDomainValidator(any());
    }

    @Test
    public void validate() {
        initializeSuppliedImage();
        assertTrue(getCommand().validate());
    }

    @Test
    public void validateCantUploadLockedImage() {
        initializeSuppliedImage();
        doReturn(new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_DISKS_LOCKED, ""))
                .when(diskImagesValidator)
                .diskImagesNotLocked();

        getCommand().validate();
        ValidateTestUtils.assertValidationMessages(
                "Can't start a transfer for a locked image.",
                getCommand(),
                EngineMessage.ACTION_TYPE_FAILED_DISKS_LOCKED);
    }

    @Test
    public void validateCantUploadDiskAttached() {
        initializeSuppliedImage();
        doReturn(new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_DISK_PLUGGED_TO_NON_DOWN_VMS, ""))
                .when(diskValidator)
                .isDiskPluggedToAnyNonDownVm(false);

        getCommand().validate();
        ValidateTestUtils.assertValidationMessages(
                "Can't start a transfer for an image that is attached to any VMs.",
                getCommand(),
                EngineMessage.ACTION_TYPE_FAILED_DISK_PLUGGED_TO_NON_DOWN_VMS);
    }

    @Test
    public void validateCantUploadDiskNotExists() {
        initializeSuppliedImage();
        doReturn(new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_DISK_NOT_EXIST, ""))
                .when(diskValidator)
                .isDiskExists();

        getCommand().validate();
        ValidateTestUtils.assertValidationMessages(
                "Can't start a transfer for image that doesn't exist.",
                getCommand(),
                EngineMessage.ACTION_TYPE_FAILED_DISK_NOT_EXIST);
    }

    @Test
    public void validateCantUploadIllegalImage() {
        initializeSuppliedImage();
        doReturn(new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_DISKS_ILLEGAL, ""))
                .when(diskImagesValidator)
                .diskImagesNotIllegal();

        getCommand().validate();
        ValidateTestUtils.assertValidationMessages(
                "Can't start a transfer for an illegal image.",
                getCommand(),
                EngineMessage.ACTION_TYPE_FAILED_DISKS_ILLEGAL);
    }

    @Test
    public void validateCantUploadToNonActiveDomain() {
        initializeSuppliedImage();
        doReturn(new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_STORAGE_DOMAIN_STATUS_ILLEGAL2, ""))
                .when(storageDomainValidator)
                .isDomainExistAndActive();

        ValidateTestUtils.runAndAssertValidateFailure(
                "Can't start a transfer to a non-active storage domain.",
                getCommand(),
                EngineMessage.ACTION_TYPE_FAILED_STORAGE_DOMAIN_STATUS_ILLEGAL2);
    }

    @Test
    public void testPermissionSubjectOnProvidedImage() {
        initializeSuppliedImage();
        assertEquals(getCommand().getPermissionCheckSubjects().get(0),
                new PermissionSubject(getCommand().getParameters().getImageGroupID(),
                        VdcObjectType.Disk,
                        ActionGroup.EDIT_DISK_PROPERTIES));
    }

    @Test
    public void testPermissionSubjectOnNewImage() {
        assertEquals(getCommand().getPermissionCheckSubjects().get(0),
                new PermissionSubject(getCommand().getParameters().getImageId(),
                        VdcObjectType.Storage,
                        ActionGroup.CREATE_DISK));
    }

    @Test
    public void testNotCreatingImageIfSupplied() {
        initializeSuppliedImage();
        doNothing().when(getCommand()).handleImageIsReadyForTransfer();
        getCommand().executeCommand();

        // Make sure no image is created if an image Guid is supplied.
        verify(getCommand(), never()).createImage();

        // Make sure that a transfer session will start.
        verify(getCommand(), times(1)).handleImageIsReadyForTransfer();
    }

    private TransferDiskImageCommand<TransferDiskImageParameters> getCommand() {
        return (TransferDiskImageCommand) transferImageCommand;
    }
}
