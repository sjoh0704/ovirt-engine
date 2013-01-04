package org.ovirt.engine.ui.webadmin.section.main.view.tab.disk;

import java.util.Date;

import javax.inject.Inject;

import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.DiskSizeColumn;
import org.ovirt.engine.ui.common.widget.table.column.FullDateTimeColumn;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.models.disks.DiskListModel;
import org.ovirt.engine.ui.uicommonweb.models.disks.DiskTemplateListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.disk.SubTabDiskTemplatePresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractSubTabTableView;

import com.google.gwt.core.client.GWT;

public class SubTabDiskTemplateView extends AbstractSubTabTableView<Disk, VmTemplate, DiskListModel, DiskTemplateListModel>
        implements SubTabDiskTemplatePresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<SubTabDiskTemplateView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public SubTabDiskTemplateView(SearchableDetailModelProvider<VmTemplate, DiskListModel, DiskTemplateListModel> modelProvider, ApplicationConstants constants) {
        super(modelProvider);
        ViewIdHandler.idHandler.generateAndSetIds(this);
        initTable(constants);
        initWidget(getTable());
    }

    void initTable(ApplicationConstants constants) {
        TextColumnWithTooltip<VmTemplate> nameColumn = new TextColumnWithTooltip<VmTemplate>() {
            @Override
            public String getValue(VmTemplate object) {
                return object.getname();
            }
        };
        getTable().addColumn(nameColumn, constants.nameTemplate());

        TextColumnWithTooltip<VmTemplate> disksColumn = new TextColumnWithTooltip<VmTemplate>() {
            @Override
            public String getValue(VmTemplate object) {
                return String.valueOf(object.getDiskMap().size());
            }
        };
        getTable().addColumn(disksColumn, constants.disksTemplate());

        DiskSizeColumn<VmTemplate> sizeColumn = new DiskSizeColumn<VmTemplate>() {
            @Override
            protected Long getRawValue(VmTemplate object) {
                return Double.valueOf(object.getActualDiskSize()).longValue();
            }
        };

        getTable().addColumn(sizeColumn, constants.actualSizeTemplate());

        FullDateTimeColumn<VmTemplate> dateColumn = new FullDateTimeColumn<VmTemplate>() {
            @Override
            protected Date getRawValue(VmTemplate object) {
                return object.getCreationDate();
            }
        };
        getTable().addColumn(dateColumn, constants.creationDateTemplate());
    }

}
